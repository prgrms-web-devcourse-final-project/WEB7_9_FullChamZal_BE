package back.fcz.domain.openai.moderation.service;

import back.fcz.domain.openai.moderation.client.OpenAiModerationClient;
import back.fcz.domain.openai.moderation.dto.CapsuleModerationBlockedPayload;
import back.fcz.domain.openai.moderation.dto.ModerationField;
import back.fcz.domain.openai.moderation.dto.OpenAiModerationResult;
import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.entity.ModerationAuditLog;
import back.fcz.domain.openai.moderation.entity.ModerationDecision;
import back.fcz.domain.openai.moderation.repository.ModerationAuditLogRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CapsuleModerationService {

    private final OpenAiModerationClient openAiModerationClient;
    private final ModerationAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * @return auditId (PASS/SKIPPED일 때 attach 위해 반환)
     */
    @Transactional
    public Long validateCapsuleText(
            Long actorMemberId,
            ModerationActionType actionType,
            String title,
            String content,
            String receiverNickname,
            String locationName,
            String address
    ) {
        LinkedHashMap<ModerationField, String> fields = new LinkedHashMap<>();
        fields.put(ModerationField.TITLE, title);
        fields.put(ModerationField.CONTENT, content);
        fields.put(ModerationField.RECEIVER_NICKNAME, receiverNickname);
        fields.put(ModerationField.LOCATION_NAME, locationName);
        fields.put(ModerationField.ADDRESS, address);

        String combinedInput = buildCombinedInput(fields);
        String inputHash = sha256Hex(combinedInput);

        // 입력이 사실상 없으면 SKIPPED
        if (combinedInput.isBlank()) {
            ModerationAuditLog logEntity = ModerationAuditLog.skipped(
                    actorMemberId,
                    actionType,
                    null,
                    openAiModerationClient.getModel(),
                    inputHash,
                    "empty input"
            );
            return auditLogRepository.save(logEntity).getId();
        }

        try {
            // 1) 전체 검사
            OpenAiModerationResult overall = openAiModerationClient.moderateText(combinedInput);

            if (!overall.flagged()) {
                ModerationAuditLog logEntity = ModerationAuditLog.success(
                        actorMemberId,
                        actionType,
                        null,
                        openAiModerationClient.getModel(),
                        false,
                        ModerationDecision.PASS,
                        inputHash,
                        openAiModerationClient.toRawJson(overall.raw())
                );
                return auditLogRepository.save(logEntity).getId();
            }

            // 2) flagged → 필드별 재검사(어느 필드가 문제인지)
            List<CapsuleModerationBlockedPayload.Violation> violations = new ArrayList<>();

            ObjectNode combinedRaw = objectMapper.createObjectNode();
            combinedRaw.set("overall", overall.raw());

            ObjectNode byFieldRaw = objectMapper.createObjectNode();

            for (Map.Entry<ModerationField, String> entry : fields.entrySet()) {
                String v = normalize(entry.getValue());
                if (v.isBlank()) continue;

                OpenAiModerationResult perField = openAiModerationClient.moderateText(v);
                byFieldRaw.set(entry.getKey().name(), perField.raw());

                if (perField.flagged()) {
                    violations.add(CapsuleModerationBlockedPayload.Violation.builder()
                            .field(entry.getKey())
                            .categories(perField.categories())
                            .build());
                }
            }

            combinedRaw.set("byField", byFieldRaw);

            // 희박하게 overall만 flagged이고 필드가 안 잡힐 수 있음 → fallback
            if (violations.isEmpty()) {
                violations.add(CapsuleModerationBlockedPayload.Violation.builder()
                        .field(ModerationField.CONTENT)
                        .categories(overall.categories())
                        .build());
            }

            ModerationAuditLog logEntity = ModerationAuditLog.success(
                    actorMemberId,
                    actionType,
                    null,
                    openAiModerationClient.getModel(),
                    true,
                    ModerationDecision.FLAGGED,
                    inputHash,
                    openAiModerationClient.toRawJson(combinedRaw)
            );
            Long auditId = auditLogRepository.save(logEntity).getId();

            CapsuleModerationBlockedPayload payload = CapsuleModerationBlockedPayload.builder()
                    .auditId(auditId)
                    .violations(violations)
                    .build();

            String message = buildBlockedMessage(violations);
            throw new BusinessException(ErrorCode.CAPSULE_CONTENT_BLOCKED, message, payload);

        } catch (RestClientException e) {
            ModerationAuditLog logEntity = ModerationAuditLog.error(
                    actorMemberId,
                    actionType,
                    null,
                    openAiModerationClient.getModel(),
                    inputHash,
                    e.getMessage()
            );
            Long auditId = auditLogRepository.save(logEntity).getId();

            Map<String, Object> payload = Map.of(
                    "auditId", auditId,
                    "reason", "OPENAI_CALL_FAILED"
            );
            throw new BusinessException(ErrorCode.OPENAI_MODERATION_FAILED, payload);
        }
    }

    @Transactional
    public void attachCapsuleId(Long auditId, Long capsuleId) {
        if (auditId == null || capsuleId == null) return;
        auditLogRepository.attachCapsuleId(auditId, capsuleId);
    }

    private String buildBlockedMessage(List<CapsuleModerationBlockedPayload.Violation> violations) {
        String fields = violations.stream()
                .map(v -> v.getField().name())
                .distinct()
                .collect(Collectors.joining(", "));
        return "유해한 내용이 감지되어 캡슐을 저장할 수 없습니다. 문제가 된 항목: " + fields;
    }

    private String buildCombinedInput(LinkedHashMap<ModerationField, String> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ModerationField, String> e : fields.entrySet()) {
            String v = normalize(e.getValue());
            if (v.isBlank()) continue;

            if (!sb.isEmpty()) sb.append("\n");
            sb.append(e.getKey().name()).append(": ").append(v);
        }
        return sb.toString();
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            log.warn("sha256Hex failed: {}", e.getMessage());
            return "HASH_FAILED";
        }
    }
}

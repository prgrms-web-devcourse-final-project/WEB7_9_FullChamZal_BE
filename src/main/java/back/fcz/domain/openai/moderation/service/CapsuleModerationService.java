package back.fcz.domain.openai.moderation.service;

import back.fcz.domain.openai.moderation.client.OpenAiModerationClient;
import back.fcz.domain.openai.moderation.client.dto.OpenAiModerationResponse;
import back.fcz.domain.openai.moderation.entity.*;
import back.fcz.domain.openai.moderation.repository.ModerationAuditLogRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class CapsuleModerationService {

    private final ObjectProvider<OpenAiModerationClient> clientProvider;
    private final ModerationAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.moderation.model:omni-moderation-2024-09-26}")
    private String model;

    // ✅ 지금 요구사항: "일단 통과" => 기본 false
    @Value("${openai.moderation.block-flagged:false}")
    private boolean blockFlagged;

    // ✅ OpenAI 장애 시에도 통과시키고 기록 남기기
    @Value("${openai.moderation.fail-open:true}")
    private boolean failOpen;

    /**
     * @return 생성 시 auditLogId(attachCapsuleId 용). 업데이트는 capsuleId 이미 있으니 null 허용.
     */
    public Long validateCapsuleText(
            Long actorMemberId,
            ModerationActionType actionType,
            String capsuleUuid,
            Long capsuleId,
            String title,
            String content,
            String receiverNickname,
            String locationName,
            String address
    ) {
        String input = buildInput(title, content, receiverNickname, locationName, address);
        String inputHash = sha256(input);
        String preview = truncate(input, 1000);

        OpenAiModerationClient client = clientProvider.getIfAvailable();

        // 클라이언트 미주입(또는 api-key 미설정) 대비: 스킵 로그 남김
        if (client == null) {
            ModerationAuditLog saved = auditLogRepository.save(
                    ModerationAuditLog.builder()
                            .capsuleId(capsuleId)
                            .capsuleUuid(capsuleUuid)
                            .actorMemberId(actorMemberId)
                            .actionType(actionType)
                            .decision(ModerationDecision.SKIP_NO_CLIENT)
                            .model(model)
                            .inputHash(inputHash)
                            .inputPreview(preview)
                            .flagged(false)
                            .errorMessage("OpenAiModerationClient not available")
                            .build()
            );
            return saved.getId();
        }

        try {
            OpenAiModerationResponse resp = client.moderate(model, input);
            OpenAiModerationResponse.Result r = (resp.getResults() != null && !resp.getResults().isEmpty())
                    ? resp.getResults().get(0)
                    : null;

            boolean flagged = r != null && r.isFlagged();

            ModerationDecision decision =
                    flagged && blockFlagged ? ModerationDecision.BLOCK : ModerationDecision.ALLOW;

            String categoriesJson = (r == null) ? null : objectMapper.writeValueAsString(r.getCategories());
            String scoresJson = (r == null) ? null : objectMapper.writeValueAsString(r.getCategoryScores());

            ModerationAuditLog saved = auditLogRepository.save(
                    ModerationAuditLog.builder()
                            .capsuleId(capsuleId)
                            .capsuleUuid(capsuleUuid)
                            .actorMemberId(actorMemberId)
                            .actionType(actionType)
                            .decision(decision)
                            .model(model)
                            .inputHash(inputHash)
                            .inputPreview(preview)
                            .flagged(flagged)
                            .categoriesJson(categoriesJson)
                            .categoryScoresJson(scoresJson)
                            .openaiModerationId(resp.getId())
                            .build()
            );

            if (flagged && blockFlagged) {
                throw new BusinessException(ErrorCode.CAPSULE_CONTENT_BLOCKED);
            }

            return saved.getId();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // OpenAI 에러/네트워크 장애 등
            ModerationDecision decision = failOpen ? ModerationDecision.FAIL_OPEN : ModerationDecision.BLOCK;

            ModerationAuditLog saved = auditLogRepository.save(
                    ModerationAuditLog.builder()
                            .capsuleId(capsuleId)
                            .capsuleUuid(capsuleUuid)
                            .actorMemberId(actorMemberId)
                            .actionType(actionType)
                            .decision(decision)
                            .model(model)
                            .inputHash(inputHash)
                            .inputPreview(preview)
                            .flagged(false)
                            .errorMessage(e.getMessage())
                            .build()
            );

            if (!failOpen) {
                throw new BusinessException(ErrorCode.OPENAI_MODERATION_FAILED);
            }

            return saved.getId();
        }
    }

    public void attachCapsuleId(Long auditLogId, Long capsuleId) {
        if (auditLogId == null || capsuleId == null) return;
        auditLogRepository.attachCapsuleId(auditLogId, capsuleId);
    }

    private String buildInput(String title, String content, String receiverNickname, String locationName, String address) {
        StringBuilder sb = new StringBuilder();

        append(sb, "title", title);
        append(sb, "content", content);
        append(sb, "receiverNickname", receiverNickname);
        append(sb, "locationName", locationName);
        append(sb, "address", address);

        return sb.toString().trim();
    }

    private void append(StringBuilder sb, String key, String value) {
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        sb.append(key).append(": ").append(v).append("\n");
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            // 해싱 실패는 시스템 에러로 보는 게 맞음
            throw new BusinessException(ErrorCode.HASHING_FAILED);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}

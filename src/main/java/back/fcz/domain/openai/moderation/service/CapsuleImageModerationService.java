package back.fcz.domain.openai.moderation.service;

import back.fcz.domain.openai.moderation.client.OpenAiModerationClient;
import back.fcz.domain.openai.moderation.dto.CapsuleImageModerationBlockedPayload;
import back.fcz.domain.openai.moderation.dto.ModerationField;
import back.fcz.domain.openai.moderation.dto.ModerationViolation;
import back.fcz.domain.openai.moderation.dto.OpenAiModerationResult;
import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.entity.ModerationAuditLog;
import back.fcz.domain.openai.moderation.entity.ModerationDecision;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CapsuleImageModerationService {

    private final OpenAiModerationClient openAiModerationClient;
    private final ModerationAuditLogWriter auditLogWriter;

    @Value("${openai.moderation.block-on-flagged:true}")
    private boolean blockOnFlagged;

    @Value("${openai.moderation.fail-closed:false}")
    private boolean failClosed;

    /**
     * 캡슐 생성/수정 시 이미지 URL 1개를 검증한다.
     * - blockOnFlagged=true면 flagged 즉시 예외로 저장 차단
     * - blockOnFlagged=false면 감사로그만 남기고 통과
     */
    public void validateImageUrl(Long actorMemberId, ModerationActionType actionType, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        String inputHash = sha256Hex(imageUrl);

        try {
            OpenAiModerationResult result = openAiModerationClient.moderateImageUrl(imageUrl);

            // ✅ PASS면 저장 안 함 (CapsuleModerationService와 동일한 정책)
            if (!result.flagged()) return;

            ModerationAuditLog logEntity = ModerationAuditLog.success(
                    actorMemberId,
                    actionType,
                    null, // capsuleId는 생성 전이면 null
                    openAiModerationClient.getModel(),
                    true,
                    ModerationDecision.FLAGGED,
                    inputHash,
                    openAiModerationClient.toRawJson(result.raw())
            );
            Long auditId = auditLogWriter.saveAndReturnId(logEntity);

            log.warn("[Moderation][IMAGE] flagged. actor={}, auditId={}, categories={}, blockOnFlagged={}",
                    actorMemberId, auditId, result.categories(), blockOnFlagged);

            if (blockOnFlagged) {
                CapsuleImageModerationBlockedPayload payload = CapsuleImageModerationBlockedPayload.builder()
                        .auditId(auditId)
                        .imageUrl(imageUrl)
                        .violations(List.of(
                                ModerationViolation.builder()
                                        // ✅ ModerationViolation.field 는 String
                                        .field(ModerationField.IMAGE_URL.name())
                                        .categories(result.categories())
                                        .build()
                        ))
                        .build();

                throw new BusinessException(ErrorCode.CAPSULE_CONTENT_BLOCKED, payload);
            }

        } catch (RestClientException e) {
            ModerationAuditLog logEntity = ModerationAuditLog.error(
                    actorMemberId,
                    actionType,
                    null,
                    openAiModerationClient.getModel(),
                    inputHash,
                    e.getMessage()
            );
            Long auditId = auditLogWriter.saveAndReturnId(logEntity);

            log.error("[Moderation][IMAGE] OpenAI call failed. actor={}, auditId={}, msg={}",
                    actorMemberId, auditId, e.getMessage(), e);

            if (failClosed) {
                throw new BusinessException(ErrorCode.OPENAI_MODERATION_FAILED);
            }
        }
    }

    /** 여러 장이면 서비스 호출자가 루프 안 돌려도 되게 제공 */
    public void validateImageUrls(Long actorMemberId, ModerationActionType actionType, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        for (String url : imageUrls) {
            validateImageUrl(actorMemberId, actionType, url);
        }
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

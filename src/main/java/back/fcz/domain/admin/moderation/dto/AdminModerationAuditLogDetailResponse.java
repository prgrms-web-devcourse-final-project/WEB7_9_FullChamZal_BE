package back.fcz.domain.admin.moderation.dto;

import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.entity.ModerationAuditLog;
import back.fcz.domain.openai.moderation.entity.ModerationDecision;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminModerationAuditLogDetailResponse {

    private final Long id;
    private final LocalDateTime createdAt;

    private final Long capsuleId;
    private final Long actorMemberId;

    private final ModerationActionType actionType;
    private final ModerationDecision decision;

    private final String model;
    private final boolean flagged;

    private final String inputHash;
    private final String rawResponseJson;
    private final String errorMessage;

    public static AdminModerationAuditLogDetailResponse from(ModerationAuditLog log) {
        return AdminModerationAuditLogDetailResponse.builder()
                .id(log.getId())
                .createdAt(log.getCreatedAt())
                .capsuleId(log.getCapsuleId())
                .actorMemberId(log.getActorMemberId())
                .actionType(log.getActionType())
                .decision(log.getDecision())
                .model(log.getModel())
                .flagged(log.isFlagged())
                .inputHash(log.getInputHash())
                .rawResponseJson(log.getRawResponseJson())
                .errorMessage(log.getErrorMessage())
                .build();
    }
}

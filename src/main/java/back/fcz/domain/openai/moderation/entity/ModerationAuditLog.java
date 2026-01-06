package back.fcz.domain.openai.moderation.entity;

import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "moderation_audit_log")
public class ModerationAuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 캡슐 생성 직전에는 null일 수 있어서 Long FK로만 들고감 (나중에 attach)
    @Column(name = "capsule_id")
    private Long capsuleId;

    @Column(name = "actor_member_id")
    private Long actorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ModerationActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 30)
    private ModerationDecision decision;

    @Column(name = "model", nullable = false, length = 64)
    private String model;

    @Column(name = "flagged", nullable = false)
    private boolean flagged;

    // 입력 원문은 저장하지 않고 해시만 저장(개인정보/콘텐츠 중복 저장 방지)
    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    // OpenAI 응답을 그대로 저장(관리자 검토/추적용)
    @Lob
    @Column(name = "raw_response_json", columnDefinition = "TEXT")
    private String rawResponseJson;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public static ModerationAuditLog skipped(
            Long actorMemberId,
            ModerationActionType actionType,
            Long capsuleId,
            String model,
            String inputHash,
            String reason
    ) {
        return ModerationAuditLog.builder()
                .actorMemberId(actorMemberId)
                .actionType(actionType)
                .capsuleId(capsuleId)
                .model(model)
                .decision(ModerationDecision.SKIPPED)
                .flagged(false)
                .inputHash(inputHash)
                .rawResponseJson(null)
                .errorMessage(reason)
                .build();
    }

    public static ModerationAuditLog success(
            Long actorMemberId,
            ModerationActionType actionType,
            Long capsuleId,
            String model,
            boolean flagged,
            ModerationDecision decision,
            String inputHash,
            String rawResponseJson
    ) {
        return ModerationAuditLog.builder()
                .actorMemberId(actorMemberId)
                .actionType(actionType)
                .capsuleId(capsuleId)
                .model(model)
                .decision(decision)
                .flagged(flagged)
                .inputHash(inputHash)
                .rawResponseJson(rawResponseJson)
                .errorMessage(null)
                .build();
    }

    public static ModerationAuditLog error(
            Long actorMemberId,
            ModerationActionType actionType,
            Long capsuleId,
            String model,
            String inputHash,
            String errorMessage
    ) {
        return ModerationAuditLog.builder()
                .actorMemberId(actorMemberId)
                .actionType(actionType)
                .capsuleId(capsuleId)
                .model(model)
                .decision(ModerationDecision.ERROR)
                .flagged(false)
                .inputHash(inputHash)
                .rawResponseJson(null)
                .errorMessage(errorMessage)
                .build();
    }
}

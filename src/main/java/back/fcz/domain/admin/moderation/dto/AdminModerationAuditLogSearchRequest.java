package back.fcz.domain.admin.moderation.dto;

import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.entity.ModerationDecision;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminModerationAuditLogSearchRequest {

    private final int page;
    private final int size;

    private final ModerationDecision decision;
    private final ModerationActionType actionType;

    private final Long actorMemberId;
    private final Long capsuleId;

    private final LocalDateTime from;
    private final LocalDateTime to;

    public static AdminModerationAuditLogSearchRequest from(
            Integer page,
            Integer size,
            ModerationDecision decision,
            ModerationActionType actionType,
            Long actorMemberId,
            Long capsuleId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return AdminModerationAuditLogSearchRequest.builder()
                .page(page != null ? page : 0)
                .size(size != null ? size : 20)
                .decision(decision)
                .actionType(actionType)
                .actorMemberId(actorMemberId)
                .capsuleId(capsuleId)
                .from(from)
                .to(to)
                .build();
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}

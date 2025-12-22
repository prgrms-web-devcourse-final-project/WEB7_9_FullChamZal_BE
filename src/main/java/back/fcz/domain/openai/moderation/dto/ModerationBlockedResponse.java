package back.fcz.domain.openai.moderation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ModerationBlockedResponse {
    private final Long auditId; // moderation_audit_log PK (있으면 내려주기)
    private final List<ModerationViolation> violations; // 어떤 필드가 어떤 카테고리로 막혔는지
}

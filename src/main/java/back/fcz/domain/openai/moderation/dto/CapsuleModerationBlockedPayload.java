package back.fcz.domain.openai.moderation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CapsuleModerationBlockedPayload {

    private final Long auditId;
    private final List<Violation> violations;

    @Getter
    @Builder
    public static class Violation {
        private final ModerationField field;
        private final List<String> categories; // true인 카테고리 key만
    }
}

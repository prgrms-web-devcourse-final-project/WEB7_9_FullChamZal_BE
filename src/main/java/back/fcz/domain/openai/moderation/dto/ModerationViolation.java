package back.fcz.domain.openai.moderation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ModerationViolation {
    private final String field;          // title/content/receiverNickname/locationName/address
    private final List<String> categories; // sexual, hate, harassment ... 등
}

package back.fcz.domain.openai.moderation.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OpenAiModerationRequest {
    private final String model; // omni-moderation-2024-09-26 등
    private final Object input; // String 또는 Array
}

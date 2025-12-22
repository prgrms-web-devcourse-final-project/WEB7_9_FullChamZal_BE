package back.fcz.domain.openai.moderation;

import back.fcz.domain.openai.moderation.client.dto.OpenAiModerationResponse;
import lombok.Getter;

@Getter
public class OpenAiModerationResult {

    private final boolean success;
    private final OpenAiModerationResponse response;
    private final String errorMessage;

    private OpenAiModerationResult(boolean success, OpenAiModerationResponse response, String errorMessage) {
        this.success = success;
        this.response = response;
        this.errorMessage = errorMessage;
    }

    public static OpenAiModerationResult success(OpenAiModerationResponse response) {
        return new OpenAiModerationResult(true, response, null);
    }

    public static OpenAiModerationResult failure(String errorMessage) {
        return new OpenAiModerationResult(false, null, errorMessage);
    }
}

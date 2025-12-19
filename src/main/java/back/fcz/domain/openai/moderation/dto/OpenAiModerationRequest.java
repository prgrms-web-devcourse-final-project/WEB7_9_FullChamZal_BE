package back.fcz.domain.openai.moderation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiModerationRequest {
    private String model;
    private Object input; // String 또는 (추후) List<멀티모달 객체>
}

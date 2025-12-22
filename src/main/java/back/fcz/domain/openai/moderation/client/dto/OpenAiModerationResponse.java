package back.fcz.domain.openai.moderation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class OpenAiModerationResponse {
    private String id;
    private String model;
    private List<Result> results;

    @Getter
    public static class Result {
        private boolean flagged;
        private Map<String, Boolean> categories;

        @JsonProperty("category_scores")
        private Map<String, Double> categoryScores;
    }
}

package back.fcz.domain.openai.moderation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class OpenAiModerationResponse {

    private List<Result> results;

    @Getter
    @NoArgsConstructor
    public static class Result {
        private Boolean flagged;
        private Map<String, Boolean> categories;

        @JsonProperty("category_scores")
        private Map<String, Double> categoryScores;
    }

    public boolean anyFlagged() {
        if (results == null) return false;
        return results.stream().anyMatch(r -> Boolean.TRUE.equals(r.getFlagged()));
    }
}

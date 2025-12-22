package back.fcz.domain.openai.moderation.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record OpenAiModerationResult(
        boolean flagged,
        List<String> categories,
        JsonNode raw
) {}

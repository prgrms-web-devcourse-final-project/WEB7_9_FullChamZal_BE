package back.fcz.domain.openai.moderation.client;

import back.fcz.domain.openai.moderation.dto.OpenAiModerationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class OpenAiModerationClient {

    private static final String MODERATIONS_URL = "https://api.openai.com/v1/moderations";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Getter
    private final String model;

    private final String apiKey;

    public OpenAiModerationClient(
            RestTemplateBuilder builder,
            ObjectMapper objectMapper,
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.moderation.model:omni-moderation-2024-09-26}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;

        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    public OpenAiModerationResult moderateText(String input) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", input);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    MODERATIONS_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            String rawJson = response.getBody();
            if (rawJson == null || rawJson.isBlank()) {
                throw new RestClientException("OpenAI moderation response body is empty");
            }

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode result0 = root.path("results").isArray() && root.path("results").size() > 0
                    ? root.path("results").get(0)
                    : null;

            boolean flagged = result0 != null && result0.path("flagged").asBoolean(false);

            List<String> trueCategories = new ArrayList<>();
            if (result0 != null) {
                JsonNode categories = result0.path("categories");
                if (categories != null && categories.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> it = categories.fields();
                    while (it.hasNext()) {
                        Map.Entry<String, JsonNode> e = it.next();
                        if (e.getValue().asBoolean(false)) {
                            trueCategories.add(e.getKey());
                        }
                    }
                }
            }

            return new OpenAiModerationResult(flagged, trueCategories, root);

        } catch (RestClientException e) {
            log.warn("OpenAI moderation call failed: {}", e.getMessage());
            throw e;
        } catch (JsonProcessingException e) {
            log.warn("OpenAI moderation JSON parse failed: {}", e.getMessage());
            throw new RestClientException("OpenAI moderation response parse failed", e);
        }
    }

    public String toRawJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}

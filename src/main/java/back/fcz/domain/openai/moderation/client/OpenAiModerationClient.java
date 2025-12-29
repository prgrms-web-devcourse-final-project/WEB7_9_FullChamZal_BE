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

    /** 텍스트 moderation  */
    public OpenAiModerationResult moderateText(String input) {
        if (input == null) input = "";
        return callModeration(input);
    }

    /** ✅ 이미지 URL moderation 추가 */
    public OpenAiModerationResult moderateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            // 호출자가 보통 null/blank를 걸러주지만, 안전장치로 PASS 처리
            return new OpenAiModerationResult(false, List.of(), null);
        }

        Map<String, Object> image = new LinkedHashMap<>();
        image.put("type", "image_url");
        image.put("image_url", Map.of("url", imageUrl));

        // OpenAI Moderation API는 input에 "멀티모달 배열"을 받을 수 있음
        List<Object> input = List.of(image);

        return callModeration(input);
    }

    /** 공통 호출 로직 (input이 String이든, List<Object>든 그대로 body에 넣음) */
    private OpenAiModerationResult callModeration(Object input) {
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

            JsonNode results = root.path("results");
            JsonNode result0 = (results.isArray() && results.size() > 0) ? results.get(0) : null;

            boolean flagged = result0 != null && result0.path("flagged").asBoolean(false);

            List<String> trueCategories = extractTrueCategories(result0);

            return new OpenAiModerationResult(flagged, trueCategories, root);

        } catch (RestClientException e) {
            log.warn("OpenAI moderation call failed: {}", e.getMessage());
            throw e;
        } catch (JsonProcessingException e) {
            log.warn("OpenAI moderation JSON parse failed: {}", e.getMessage());
            throw new RestClientException("OpenAI moderation response parse failed", e);
        }
    }

    private List<String> extractTrueCategories(JsonNode result0) {
        List<String> trueCategories = new ArrayList<>();
        if (result0 == null) return trueCategories;

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
        return trueCategories;
    }

    public String toRawJson(JsonNode node) {
        if (node == null) return null;
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}

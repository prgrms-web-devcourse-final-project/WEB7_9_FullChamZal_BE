package back.fcz.domain.openai.moderation;

import back.fcz.domain.openai.moderation.client.dto.OpenAiModerationRequest;
import back.fcz.domain.openai.moderation.client.dto.OpenAiModerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiModerationClient {

    private final RestTemplate openAiRestTemplate;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.moderation.model:omni-moderation-2024-09-26}")
    private String model;

    public OpenAiModerationResult moderateText(String input) {
        String url = baseUrl + "/moderations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        OpenAiModerationRequest body = new OpenAiModerationRequest(model, input);
        HttpEntity<OpenAiModerationRequest> request = new HttpEntity<>(body, headers);

        try {
            OpenAiModerationResponse response =
                    openAiRestTemplate.postForObject(url, request, OpenAiModerationResponse.class);

            return OpenAiModerationResult.success(response);
        } catch (RestClientException e) {
            // ✅ fail-open 정책: 여기서 예외 던지지 않고 실패 결과로 반환
            String msg = e.getMessage();
            log.warn("[OpenAI Moderation] request failed: {}", msg);
            return OpenAiModerationResult.failure(msg);
        }
    }
}

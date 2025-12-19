package back.fcz.domain.openai.moderation;

import back.fcz.domain.openai.moderation.dto.OpenAiModerationRequest;
import back.fcz.domain.openai.moderation.dto.OpenAiModerationResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

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

    @Value("${openai.moderation.fail-closed:true}")
    private boolean failClosed;

    public Optional<OpenAiModerationResponse> moderateText(String input) {
        String url = baseUrl + "/moderations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        OpenAiModerationRequest body = new OpenAiModerationRequest(model, input);
        HttpEntity<OpenAiModerationRequest> request = new HttpEntity<>(body, headers);

        try {
            OpenAiModerationResponse response =
                    openAiRestTemplate.postForObject(url, request, OpenAiModerationResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException e) {
            log.warn("[OpenAI Moderation] request failed: {}", e.getMessage());
            if (failClosed) {
                // OpenAI 장애면 저장 자체를 막는 정책
                throw new BusinessException(ErrorCode.OPENAI_MODERATION_FAILED);
            }
            // fail-open 정책이면 그냥 통과 처리
            return Optional.empty();
        }
    }
}

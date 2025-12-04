package back.fcz.global.config.swagger;

import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Configuration
public class SwaggerConfig {

    /**
     * 커스텀 어노테이션이 붙은 메서드에 대해 Swagger 응답 예시를 자동으로 생성합니다.
     */
    @Bean
    public OperationCustomizer customize() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            // 메서드에 @ApiErrorCodeExample 어노테이션이 있는지 확인
            ApiErrorCodeExample apiErrorCodeExample =
                    handlerMethod.getMethodAnnotation(ApiErrorCodeExample.class);

            if (apiErrorCodeExample != null) {
                // 어노테이션에 지정된 ErrorCode 배열을 가져옴
                ErrorCode[] errorCodes = apiErrorCodeExample.value();
                generateErrorCodeResponseExample(operation, errorCodes);
            }

            return operation;
        };
    }

    /**
     * 지정된 ErrorCode들로 Swagger 응답 예시를 생성합니다.
     */
    private void generateErrorCodeResponseExample(
            Operation operation,
            ErrorCode[] errorCodes) {

        ApiResponses responses = operation.getResponses();

        // HTTP 상태코드별로 에러 케이스들을 그룹핑
        // 예: 400 -> [CMN001, CMN003, CMN004], 401 -> [CMN006]
        Map<Integer, List<ExampleHolder>> statusWithExampleHolders =
                Arrays.stream(errorCodes)
                        .map(errorCode -> {
                            // ApiResponse 형태로 Example 생성
                            ApiResponse<Void> errorResponse = new ApiResponse<>(
                                    errorCode.getCode(),
                                    errorCode.getMessage(),
                                    null
                            );

                            // Swagger Example 객체 생성
                            Example example = new Example();
                            example.setValue(errorResponse);
                            example.setDescription(errorCode.getMessage());

                            // ExampleHolder로 포장해서 반환
                            return ExampleHolder.builder()
                                    .example(example)
                                    .name(errorCode.getCode()) // CMN001, CMN002 등
                                    .httpStatusCode(errorCode.getStatus().value()) // 400, 401 등
                                    .build();
                        })
                        .collect(groupingBy(ExampleHolder::getHttpStatusCode));

        // 그룹핑된 결과를 Swagger responses에 추가
        addExamplesToResponses(responses, statusWithExampleHolders);
    }

    /**
     * HTTP 상태코드별로 그룹핑된 예시들을 Swagger responses에 추가합니다.
     */
    private void addExamplesToResponses(
            ApiResponses responses,
            Map<Integer, List<ExampleHolder>> statusWithExampleHolders) {

        statusWithExampleHolders.forEach((httpStatusCode, exampleHolders) -> {
            // 해당 상태코드에 대한 ApiResponse가 이미 있는지 확인
            io.swagger.v3.oas.models.responses.ApiResponse apiResponse =
                    responses.get(String.valueOf(httpStatusCode));

            // 없으면 새로 생성
            if (apiResponse == null) {
                apiResponse = new io.swagger.v3.oas.models.responses.ApiResponse();
            }

            // Content 객체 생성 (application/json)
            Content content = apiResponse.getContent();
            if (content == null) {
                content = new Content();
                apiResponse.setContent(content);
            }

            // MediaType 객체 생성
            MediaType mediaType = content.get("application/json");
            if (mediaType == null) {
                mediaType = new MediaType();
                content.addMediaType("application/json", mediaType);
            }

            // 각 에러 케이스를 Examples에 추가
            // Swagger UI에서 드롭다운으로 선택할 수 있게 됨
            for (ExampleHolder exampleHolder : exampleHolders) {
                mediaType.addExamples(
                        exampleHolder.getName(), // 예: "CMN001"
                        exampleHolder.getExample()
                );
            }

            // 최종적으로 responses에 추가
            responses.addApiResponse(String.valueOf(httpStatusCode), apiResponse);
        });
    }
}
package back.fcz.global.config.swagger;

import io.swagger.v3.oas.models.examples.Example;
import lombok.Builder;
import lombok.Getter;

/**
 * Swagger Example 정보를 담는 홀더 클래스
 */
@Getter
@Builder
public class ExampleHolder {
    private Example example;     // Swagger의 Example 객체
    private String name;          // 에러 코드명 (예: CMN001)
    private int httpStatusCode;   // HTTP 상태 코드 (예: 400)
}
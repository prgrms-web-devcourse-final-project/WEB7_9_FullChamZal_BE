package back.fcz.global.config.swagger;

import back.fcz.global.exception.ErrorCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드에 이 어노테이션을 붙이면
 * 지정된 ErrorCode들이 Swagger UI에 자동으로 표시됩니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodeExample {
    ErrorCode[] value();
}
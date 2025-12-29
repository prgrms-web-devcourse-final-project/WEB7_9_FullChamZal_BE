package back.fcz.global.exception;

import back.fcz.global.response.ApiResponse;
import back.fcz.global.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        // 필드별로 에러 메시지를 Map으로 수집
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage() != null
                    ? error.getDefaultMessage()
                    : "검증에 실패했습니다";

            // 동일한 필드에 여러 에러가 있을 경우 ", "로 연결
            errors.merge(fieldName, errorMessage, (existing, newMsg) -> existing + ", " + newMsg);
        });

        log.warn("Validation failed: {}", errors);

        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                ResponseCode.BAD_REQUEST.getCode(),
                ResponseCode.BAD_REQUEST.getMessage(),
                errors
        );

        return new ResponseEntity<>(response, ResponseCode.BAD_REQUEST.getStatus());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        // HTTP 상태 코드에 따라 로깅 레벨 구분
        if (errorCode.getStatus().is5xxServerError()) {
            // 5xx: 서버 오류 - error 레벨 (스택 트레이스 포함)
            log.error("Business exception occurred: code={}, message={}",
                    errorCode.getCode(), ex.getMessage(), ex);
        } else {
            // 4xx: 클라이언트 오류 - warn 레벨 (스택 트레이스 제외)
            log.warn("Business exception occurred: code={}, message={}",
                    errorCode.getCode(), ex.getMessage());
        }

        // ✅ message는 예외 message(오버라이드 가능)를 우선 사용
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : errorCode.getMessage();

        ApiResponse<Object> response = new ApiResponse<>(
                errorCode.getCode(),
                message,
                ex.getData() // ✅ 여기로 moderation 결과(어떤 필드/카테고리) 내려줌
        );

        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNotFound(NoResourceFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INTERNAL_ERROR);
        return new ResponseEntity<>(response, ErrorCode.INTERNAL_ERROR.getStatus());
    }

    @ExceptionHandler({
            ClientAbortException.class,
            AsyncRequestNotUsableException.class
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleClientAbortLikeExceptions(Exception e) {
        log.debug("Client connection aborted or not usable");
    }
}

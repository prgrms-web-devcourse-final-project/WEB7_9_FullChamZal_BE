package back.fcz.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ========== 공통 에러 ==========
    VALIDATION_FAILED("CMN001", HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),
    INTERNAL_ERROR("CMN002", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE("CMN003", HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INVALID_TYPE_VALUE("CMN004", HttpStatus.BAD_REQUEST, "잘못된 타입의 값입니다."),
    MISSING_REQUEST_PARAMETER("CMN005", HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다."),
    UNAUTHORIZED("CMN006", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // ========== 암호화 에러 ==========
    ENCRYPTION_FAILED("CRY001", HttpStatus.INTERNAL_SERVER_ERROR, "데이터 암호화에 실패했습니다."),
    DECRYPTION_FAILED("CRY002", HttpStatus.INTERNAL_SERVER_ERROR, "데이터 복호화에 실패했습니다."),
    HASHING_FAILED("CRY003", HttpStatus.INTERNAL_SERVER_ERROR, "데이터 해싱에 실패했습니다."),

    // ========== 캡슐 에러 ==========
    CAPSULE_NOT_FOUND("CPS001", HttpStatus.NOT_FOUND, "해당 캡슐을 찾을 수 없습니다."),

    // ========== 해제조건 에러 ==========
    INVALID_UNLOCK_TIME("UNL001", HttpStatus.BAD_REQUEST, "유효하지 않은 시간 값입니다."),
    INVALID_LATITUDE_LONGITUDE("UNL002", HttpStatus.BAD_REQUEST, "유효하지 않은 위도 또는 경도 값입니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}

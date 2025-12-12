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

    //=========== SMS 에러 ==========
    SMS_SEND_FAILED("SMS001", HttpStatus.INTERNAL_SERVER_ERROR, "문자 발송에 실패했습니다."),
    SMS_RESEND_COOLDOWN("SMS002", HttpStatus.BAD_REQUEST, "인증문자 재전송 쿨타임이 지나지 않았습니다."),
    VERIFICATION_NOT_FOUND("SMS003", HttpStatus.NOT_FOUND, "해당 전화번호에 대한 인증 내역을 찾을 수 없습니다."),
    VERIFICATION_EXPIRED("SMS004", HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다."),
    VERIFICATION_ATTEMPT_EXCEEDED("SMS005", HttpStatus.BAD_REQUEST, "인증 시도 횟수를 초과했습니다."),
    VERIFICATION_CODE_MISMATCH("SMS006", HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다."),
    // ========== JWT 인증 에러 ==========
    TOKEN_EXPIRED("AUTH001", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID("AUTH002", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_SIGNATURE_INVALID("AUTH003", HttpStatus.UNAUTHORIZED, "토큰 서명이 유효하지 않습니다."),
    TOKEN_MALFORMED("AUTH004", HttpStatus.UNAUTHORIZED, "토큰 형식이 올바르지 않습니다."),
    TOKEN_UNSUPPORTED("AUTH005", HttpStatus.UNAUTHORIZED, "지원되지 않는 토큰입니다."),
    TOKEN_EMPTY("AUTH006", HttpStatus.UNAUTHORIZED, "토큰이 비어있습니다."),
    TOKEN_BLACKLISTED("AUTH007", HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다."),
    TOKEN_USER_TYPE_MISMATCH("AUTH008", HttpStatus.FORBIDDEN, "토큰의 사용자 타입이 일치하지 않습니다."),
    TOKEN_SUBJECT_INVALID("AUTH009", HttpStatus.UNAUTHORIZED, "토큰 SUBJECT 형식이 올바르지 않습니다."),

    // ========== 권한 에러 ==========
    ACCESS_DENIED("PERM001", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INSUFFICIENT_PERMISSION("PERM002", HttpStatus.FORBIDDEN, "권한이 부족합니다."),

    // ========== Redis 에러 ==========
    REDIS_CONNECTION_ERROR("REDIS001", HttpStatus.SERVICE_UNAVAILABLE, "일시적인 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // ========== Auth 도메인 에러 ==========
    DUPLICATE_USER_ID("AUTH001", HttpStatus.CONFLICT, "이미 존재하는 아이디입니다."),
    DUPLICATE_NICKNAME("AUTH002", HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
    DUPLICATE_PHONENUM("AUTH003", HttpStatus.CONFLICT, "이미 존재하는 전화번호입니다."),
    INVALID_USER_ID("AUTH004", HttpStatus.CONFLICT, "아이디가 존재하지 않습니다."),
    INVALID_PASSWORD("AUTH005", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),

    // ========== Member 도메인 에러 ==========
    MEMBER_NOT_FOUND("MEM001", HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    MEMBER_NOT_ACTIVE("MEM002", HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),

    // ========== 캡슐 에러 ==========
    CAPSULE_NOT_FOUND("CPS001", HttpStatus.NOT_FOUND, "해당 캡슐을 찾을 수 없습니다."),
    NOT_OPENED_CAPSULE("CPS02", HttpStatus.BAD_REQUEST, "시간/위치 검증에 실패하였습니다."),
    CAPSULE_NOT_RECEIVER("CPS03", HttpStatus.FORBIDDEN, "이 캡슐의 수신자가 아닙니다."),
    CAPSULE_PASSWORD_NOT_MATCH("CPS04", HttpStatus.UNAUTHORIZED, "캡슐 비밀번호가 일치하지 않습니다."),
    CAPSULE_CONDITION_ERROR("CPS05", HttpStatus.BAD_REQUEST, "캡슐 조건 로직 에러입니다."),
    CAPSULE_NOT_CREATE("CPS006", HttpStatus.BAD_REQUEST, "캡슐을 생성할 수 없습니다."),

    // ========== 해제조건 에러 ==========
    INVALID_UNLOCK_TIME("UNL001", HttpStatus.BAD_REQUEST, "유효하지 않은 시간 값입니다."),
    INVALID_LATITUDE_LONGITUDE("UNL002", HttpStatus.BAD_REQUEST, "유효하지 않은 위도 또는 경도 값입니다."),
    INVALID_RADIUS("UNL003", HttpStatus.BAD_REQUEST, "반경 값은 null, 500m, 1km, 1.5km 중 하나여야 합니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}

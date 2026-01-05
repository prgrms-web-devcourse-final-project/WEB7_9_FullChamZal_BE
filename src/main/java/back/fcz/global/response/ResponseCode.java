package back.fcz.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ResponseCode {

    OK("200", HttpStatus.OK, "정상적으로 완료되었습니다."),
    CREATED("201", HttpStatus.CREATED, "정상적으로 생성되었습니다."),
    NO_CONTENT("204", HttpStatus.NO_CONTENT, "정상적으로 처리되었으나 반환할 내용이 없습니다."),
    BAD_REQUEST("400", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED("401", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("404", HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("405", HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    CONFLICT("409", HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),
    INTERNAL_SERVER_ERROR("500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러가 발생했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ResponseCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
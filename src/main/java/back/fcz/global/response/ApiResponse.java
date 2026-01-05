package back.fcz.global.response;

import back.fcz.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApiResponse <T> {

    private String code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.OK.getCode(), ResponseCode.OK.getMessage(), data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResponseCode.OK.getCode(), ResponseCode.OK.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ResponseCode code) {
        return new ApiResponse<>(code.getCode(), code.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ResponseCode code, String message) {
        return new ApiResponse<>(code.getCode(), message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode code) {
        return new ApiResponse<>(code.getCode(), code.getMessage(), null);
    }

    public static <T> ApiResponse<T> noContent() {
        return new ApiResponse<>(ResponseCode.NO_CONTENT.getCode(), ResponseCode.NO_CONTENT.getMessage(), null);
    }
}
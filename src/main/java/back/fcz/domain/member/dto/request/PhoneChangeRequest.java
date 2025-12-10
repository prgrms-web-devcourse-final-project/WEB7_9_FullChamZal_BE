package back.fcz.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 전화번호 변경 요청 DTO
 */
public record PhoneChangeRequest(
        @NotBlank(message = "전화번호는 필수입니다")
        @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", message = "올바른 전화번호 형식이 아닙니다")
        String phoneNumber
) {
    /**
     * 전화번호 정규화 (하이픈 제거)
     */
    public String normalizedPhoneNumber() {
        return phoneNumber.replaceAll("-", "");
    }
}
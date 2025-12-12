package back.fcz.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO
 */
public record MemberSignupRequest(
        @NotBlank(message = "아이디는 필수입니다")
        @Size(min = 4, max = 20, message = "아이디는 4~20자 사이여야 합니다")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "아이디는 영문, 숫자, 언더스코어만 가능합니다")
        String userId,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
        )
        String password,

        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 50, message = "이름은 2~50자 사이여야 합니다")
        String name,

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다")
        String nickname,

        @Schema(description = "전화번호는 필수입니다 (숫자만 11자리)", example = "01087654321")
        @Pattern(
                regexp = "^01[016789]\\d{8}$",
                message = "전화번호는 010, 011, 016~019로 시작하는 11자리 숫자여야 합니다"
        )
        @Size(min = 11, max = 11, message = "전화번호는 정확히 11자리여야 합니다.")
        String phoneNumber
) {
    /**
     * 전화번호 정규화 (하이픈 제거)
     * 예: 010-1234-5678 -> 01012345678
     */
    public String normalizedPhoneNumber() {
        return phoneNumber.replaceAll("-", "");
    }
}
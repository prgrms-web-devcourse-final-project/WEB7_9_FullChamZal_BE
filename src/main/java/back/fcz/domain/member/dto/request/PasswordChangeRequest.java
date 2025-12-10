package back.fcz.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청 DTO
 */
public record PasswordChangeRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
        )
        String newPassword
) {
    /**
     * 현재 비밀번호와 새 비밀번호가 동일한지 검증
     */
    public boolean isSamePassword() {
        return currentPassword.equals(newPassword);
    }
}
package back.fcz.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재확인 요청 DTO
 */
public record PasswordVerifyRequest(
        @NotBlank(message = "비밀번호는 필수입니다")
        String password
) {
}
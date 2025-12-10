package back.fcz.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO
 */
public record MemberLoginRequest(
        @NotBlank(message = "아이디는 필수입니다")
        String userId,

        @NotBlank(message = "비밀번호는 필수입니다")
        String password
) {
}
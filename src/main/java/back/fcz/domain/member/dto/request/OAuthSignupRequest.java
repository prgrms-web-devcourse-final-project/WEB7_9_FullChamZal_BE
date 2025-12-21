package back.fcz.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 소셜 로그인 첫 사용 회원의
 * 닉네임 설정, 전화번호 인증 요청 DTO
 */
public record OAuthSignupRequest(
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
}

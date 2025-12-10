package back.fcz.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 닉네임 변경 요청 DTO
 */
public record NicknameChangeRequest(
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다")
        String nickname
) {
}
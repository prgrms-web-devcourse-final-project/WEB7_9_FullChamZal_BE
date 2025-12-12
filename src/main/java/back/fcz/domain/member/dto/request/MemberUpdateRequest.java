package back.fcz.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원 정보 수정 요청 DTO
 */
public record MemberUpdateRequest(

        @Schema(description = "새 닉네임 (2~20자, 한글/영문/숫자)", example = "새닉네임")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 가능합니다.")
        String nickname,

        @Schema(description = "현재 비밀번호 (비밀번호 변경 시 필수)", example = "currentPass123!")
        String currentPassword,

        @Schema(description = "새 비밀번호 (8~20자, 영문+숫자+특수문자)", example = "newPass123!")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        String newPassword,

        @Schema(description = "새 전화번호 (숫자만 11자리)", example = "01087654321")
        @Pattern(
                regexp = "^01[016789]\\d{8}$",
                message = "전화번호는 010, 011, 016~019로 시작하는 11자리 숫자여야 합니다"
        )
        @Size(min = 11, max = 11, message = "전화번호는 정확히 11자리여야 합니다.")
        String phoneNumber
) {
    // 닉네임 변경 요청 확인
    public boolean hasNicknameChange() {
        return nickname != null && !nickname.isBlank();
    }

    // 비밀번호 변경 요청 확인
    public boolean hasPasswordChange() {
        return newPassword != null && !newPassword.isBlank();
    }

    // 전화번호 변경 요청 확인
    public boolean hasPhoneChange() {
        return phoneNumber != null && !phoneNumber.isBlank();
    }

    // 요청이 하나라도 있는지 확인
    public boolean hasAnyChange() {
        return hasNicknameChange() || hasPasswordChange() || hasPhoneChange();
    }
}

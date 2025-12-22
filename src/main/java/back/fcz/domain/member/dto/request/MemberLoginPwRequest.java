package back.fcz.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberLoginPwRequest(
        @Schema(description = "전화번호는 필수입니다 (숫자만 11자리)", example = "01087654321")
        @Pattern(
                regexp = "^01[016789]\\d{8}$",
                message = "전화번호는 010, 011, 016~019로 시작하는 11자리 숫자여야 합니다"
        )
        @Size(min = 11, max = 11, message = "전화번호는 정확히 11자리여야 합니다.")
        String phoneNum,

        @Schema(description = "새 비밀번호 (8~20자, 영문+숫자+특수문자)", example = "newPass123!")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        String password
) {
}

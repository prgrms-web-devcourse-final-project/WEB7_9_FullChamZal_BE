package back.fcz.domain.sms.dto.request;

import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendSmsCodeRequest(
        @NotBlank(message = "전화번호는 필수입니다")
        @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", message = "올바른 전화번호 형식이 아닙니다")
        String phoneNumber,

        PhoneVerificationPurpose purpose,

        Boolean resend
) {}

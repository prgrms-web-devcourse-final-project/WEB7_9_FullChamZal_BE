package back.fcz.domain.sms.dto.response;

public record SendSmsCodeResponse(
        boolean success,
        int cooldownSeconds
) {}

package back.fcz.domain.sms.controller;

import back.fcz.domain.sms.dto.request.ConfirmSmsCodeRequest;
import back.fcz.domain.sms.dto.request.SendSmsCodeRequest;
import back.fcz.domain.sms.dto.response.ConfirmSmsCodeResponse;
import back.fcz.domain.sms.dto.response.SendSmsCodeResponse;
import back.fcz.domain.sms.service.PhoneVerificationService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/phone-verification")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    // 인증 코드 발송 POST /api/v1/phone-verification
    @Operation(
            summary = "SMS 인증 코드 발송",
            description = "입력한 번호로 인증코드를 발송합니다 " +
                    "재전송 요청 시 30초 쿨다운이 적용됩니다"
    )
    @ApiErrorCodeExample({
            ErrorCode.SMS_SEND_FAILED,
            ErrorCode.SMS_RESEND_COOLDOWN
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SendSmsCodeResponse>> sendCode(
            @RequestBody SendSmsCodeRequest sendSmsCodeRequest
    ){
        SendSmsCodeResponse response = phoneVerificationService.sendCode(sendSmsCodeRequest);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
    // 인증 코드 검증 POST /api/v1/phone-verification/confirm
    @Operation(
            summary = "SMS 인증 코드 검증",
            description = "입력한 인증코드가 올바른지 검증합니다. 인증 코드 만료 시간은 3분입니다." +
                    "검증에 실패할 경우 최대 시도 횟수는 5회이며, 초과 시 재발송이 필요합니다"
    )
    @ApiErrorCodeExample({
            ErrorCode.VERIFICATION_NOT_FOUND,
            ErrorCode.VERIFICATION_EXPIRED,
            ErrorCode.VERIFICATION_ATTEMPT_EXCEEDED,
            ErrorCode.VERIFICATION_CODE_MISMATCH
    })
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ConfirmSmsCodeResponse>> confirmsCode(
            @RequestBody ConfirmSmsCodeRequest confirmSmsCodeRequest
    ){
        ConfirmSmsCodeResponse response = phoneVerificationService.confirmCode(confirmSmsCodeRequest);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}

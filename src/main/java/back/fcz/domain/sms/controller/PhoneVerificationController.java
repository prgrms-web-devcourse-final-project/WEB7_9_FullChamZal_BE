package back.fcz.domain.sms.controller;

import back.fcz.domain.sms.dto.request.ConfirmSmsCodeRequest;
import back.fcz.domain.sms.dto.request.SendSmsCodeRequest;
import back.fcz.domain.sms.dto.response.ConfirmSmsCodeResponse;
import back.fcz.domain.sms.dto.response.SendSmsCodeResponse;
import back.fcz.domain.sms.service.PhoneVerificationService;
import back.fcz.global.response.ApiResponse;
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
    @PostMapping
    public ResponseEntity<ApiResponse<SendSmsCodeResponse>> sendCode(
            @RequestBody SendSmsCodeRequest sendSmsCodeRequest
    ){
        SendSmsCodeResponse response = phoneVerificationService.sendCode(sendSmsCodeRequest);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
    // 인증 코드 검증 POST /api/v1/phone-verification/confirm
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ConfirmSmsCodeResponse>> confirmsCode(
            @RequestBody ConfirmSmsCodeRequest confirmSmsCodeRequest
    ){
        ConfirmSmsCodeResponse response = phoneVerificationService.confirmCode(confirmSmsCodeRequest);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}

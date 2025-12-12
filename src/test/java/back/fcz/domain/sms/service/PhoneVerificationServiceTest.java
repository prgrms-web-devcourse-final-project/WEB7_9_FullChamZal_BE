package back.fcz.domain.sms.service;

import back.fcz.domain.sms.dto.request.SendSmsCodeRequest;
import back.fcz.domain.sms.dto.response.SendSmsCodeResponse;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.repository.PhoneVerificationRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.infra.sms.CoolSmsClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {
    @InjectMocks
    private PhoneVerificationService phoneVerificationService;
    @Mock
    private PhoneVerificationRepository phoneVerificationRepository;
    @Mock
    private PhoneCrypto phoneCrypto;
    @Mock
    private CoolSmsClient coolSmsClient;

    @Test
    @DisplayName("인증 번호 발송 : 최초 - 성공")
    void sendCode_First_Success() {
        //given
        SendSmsCodeRequest request = new SendSmsCodeRequest(
                "010-1234-5678",
                PhoneVerificationPurpose.SIGNUP,
                false
        );

        when(phoneVerificationRepository
                .countByPhoneNumberHashAndPurposeAndCreatedAtAfter(anyString(),any(),any()))
                .thenReturn(0L);
        when(phoneCrypto.hash(anyString())).thenReturn("hashedPhoneNumber");

        //when
        SendSmsCodeResponse response = phoneVerificationService.sendCode(request);

        //then

    }
}

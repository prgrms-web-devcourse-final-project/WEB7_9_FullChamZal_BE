package back.fcz.domain.sms.service;

import back.fcz.domain.sms.dto.request.ConfirmSmsCodeRequest;
import back.fcz.domain.sms.dto.request.SendSmsCodeRequest;
import back.fcz.domain.sms.dto.response.ConfirmSmsCodeResponse;
import back.fcz.domain.sms.dto.response.SendSmsCodeResponse;
import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.entity.PhoneVerificationStatus;
import back.fcz.domain.sms.repository.PhoneVerificationRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.infra.sms.CoolSmsClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();

        verify(phoneVerificationRepository, times(1)).save(any(PhoneVerification.class));
        verify(coolSmsClient, times(1)).sendSms(eq("01012345678"), anyString());
    }

    @Test
    @DisplayName("인증 번호 발송 : 재전송 - 기존 보류중인 인증 건이 유효한 경우 - 성공")
    void sendCode_ResendWithValidPending_Success() {
        //given
        SendSmsCodeRequest request = new SendSmsCodeRequest(
                "010-1234-5678",
                PhoneVerificationPurpose.SIGNUP,
                true
        );

        PhoneVerification existingVerification = mock(PhoneVerification.class);

        when(phoneVerificationRepository
                .countByPhoneNumberHashAndPurposeAndCreatedAtAfter(anyString(),any(),any()))
                .thenReturn(0L);
        when(phoneVerificationRepository
                .findLatestPending(anyString(), any()))
                .thenReturn(java.util.Optional.of(existingVerification));
        when(phoneCrypto.hash(anyString())).thenReturn("hashedPhoneNumber");

        //when
        SendSmsCodeResponse response = phoneVerificationService.sendCode(request);

        //then
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();

        verify(existingVerification, times(1)).reset(anyString(), any());
        verify(phoneVerificationRepository, never()).save(any(PhoneVerification.class));
        verify(coolSmsClient, times(1)).sendSms(eq("01012345678"), anyString());
    }

    @Test
    @DisplayName("인증 번호 발송 : 재전송 - 기존 보류중인 인증 건이 만료된 경우 - 성공")
    void sendCode_ResendWithExpiredPending_Success() {
        //given
        SendSmsCodeRequest request = new SendSmsCodeRequest(
                "010-1234-5678",
                PhoneVerificationPurpose.SIGNUP,
                true
        );

        PhoneVerification existingVerification = mock(PhoneVerification.class);

        when(phoneVerificationRepository
                .countByPhoneNumberHashAndPurposeAndCreatedAtAfter(anyString(),any(),any()))
                .thenReturn(0L);
        when(phoneVerificationRepository
                .findLatestPending(anyString(), any()))
                .thenReturn(java.util.Optional.of(existingVerification));
        when(existingVerification.isExpired(any())).thenReturn(true);
        when(phoneCrypto.hash(anyString())).thenReturn("hashedPhoneNumber");

        //when
        SendSmsCodeResponse response = phoneVerificationService.sendCode(request);

        //then
        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();

        verify(existingVerification, times(1)).markExpired();
        verify(phoneVerificationRepository, times(1)).save(any(PhoneVerification.class));
        verify(coolSmsClient, times(1)).sendSms(eq("01012345678"), anyString());
    }
    @Test
    @DisplayName("인증 코드 검증 - 성공")
    void verifyCode_Success() {
        //given
        String phoneNumber = "010-1234-5678";
        String nomalizedPhoneNumber = "01012345678";
        String phoneNumberHash = "hashedPhoneNumber";
        String inputCode = "123456";

        ConfirmSmsCodeRequest request = new ConfirmSmsCodeRequest(
                phoneNumber,
                inputCode,
                PhoneVerificationPurpose.SIGNUP
        );

        PhoneVerification pv = PhoneVerification.initForTest(
                phoneNumberHash,
                "hashedCode",
                PhoneVerificationPurpose.SIGNUP,
                PhoneVerificationStatus.PENDING,
                0,
                LocalDateTime.now().minusMinutes(1),
                null,
                LocalDateTime.now().plusMinutes(2)
        );

        when(phoneCrypto.hash(nomalizedPhoneNumber)).thenReturn(phoneNumberHash);

        when(phoneVerificationRepository.findLatestPending(eq(phoneNumberHash), eq(PhoneVerificationPurpose.SIGNUP)))
                .thenReturn(java.util.Optional.of(pv));
        when(phoneCrypto.verifyHash(any(), eq("hashedCode"))).thenReturn(true);

        //when
        ConfirmSmsCodeResponse response = phoneVerificationService.confirmCode(request);

        //then
        assertThat(response.verified()).isTrue();
        assertThat(pv.getStatus()).isEqualTo(PhoneVerificationStatus.VERIFIED);
        assertThat(pv.getAttemptCount()).isEqualTo(0);
        assertThat(pv.getVerifiedAt()).isNotNull();

        verify(phoneCrypto).hash(nomalizedPhoneNumber);
        verify(phoneVerificationRepository).findLatestPending(phoneNumberHash, PhoneVerificationPurpose.SIGNUP);
    }
}

package back.fcz.domain.member.service;

import back.fcz.domain.member.dto.request.OAuthSignupRequest;
import back.fcz.domain.member.dto.response.MemberSignupResponse;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.service.PhoneVerificationService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OAuthServiceTest {
    private OAuthService oauthService;
    private MemberRepository memberRepository;
    private PhoneVerificationService phoneVerificationService;
    private PhoneCrypto phoneCrypto;

    private Member member;
    private OAuthSignupRequest request;
    private final Long memberId = 1L;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        phoneVerificationService = mock(PhoneVerificationService.class);
        phoneCrypto = mock(PhoneCrypto.class);
        oauthService = new OAuthService(memberRepository, phoneVerificationService, phoneCrypto);
    }

    @BeforeEach
    void setUpInitData() {
        member = Member.createOAuth("test@gmail.com", "tester", "tester");
        request = new OAuthSignupRequest("newNick", "01012345678");
    }

    @Test
    @DisplayName("소셜 로그인 첫 사용자의 닉네임 설정, 전화번호 인증 및 설정 성공")
    void oauthSignup_success() {
        // given
        String phoneHash = "hashed_phone";
        String encryptedPhone = "encrypted_phone";

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(phoneCrypto.hash(request.phoneNumber())).thenReturn(phoneHash);
        when(phoneCrypto.encrypt(request.phoneNumber())).thenReturn(encryptedPhone);

        // nickname 중복 여부 체크
        when(memberRepository.existsByNickname(request.nickname())).thenReturn(false);

        // 전화번호 중복 여부 체크
        when(memberRepository.existsByPhoneHashAndDeletedAtIsNull(phoneHash)).thenReturn(false);
        when(memberRepository.findByPhoneHashAndDeletedAtIsNotNull(phoneHash)).thenReturn(Optional.empty());

        // 전화번호 인증
        when(phoneVerificationService.isPhoneVerified(
                "01012345678",
                PhoneVerificationPurpose.SIGNUP
        )).thenReturn(true);

        // when
        MemberSignupResponse response = oauthService.oauthSignup(memberId, request);

        // then
        assertThat(response.memberId()).isEqualTo(memberId);
        assertThat(member.getNickname()).isEqualTo("newNick");
        assertThat(member.getPhoneHash()).isEqualTo(phoneHash);
    }

    @Test
    @DisplayName("실패 - 중복 닉네임 존재")
    void oauthSignup_duplicate_nickname() {
        // given
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(anyString())).thenReturn(true);

        // when
        BusinessException exception = assertThrows(BusinessException.class, () ->
                oauthService.oauthSignup(memberId, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("실패 - 활성 회원 중, 중복 전화번호 존재")
    void oauthSignup_duplicate_phone() {
        // given
        String phoneHash = "duplicate_hash";
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(anyString())).thenReturn(false);
        when(phoneCrypto.hash(anyString())).thenReturn(phoneHash);
        when(memberRepository.existsByPhoneHashAndDeletedAtIsNull(phoneHash)).thenReturn(true);

        // when
        BusinessException exception = assertThrows(BusinessException.class, () ->
                oauthService.oauthSignup(memberId, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PHONENUM);
    }
}

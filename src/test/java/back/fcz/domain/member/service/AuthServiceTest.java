package back.fcz.domain.member.service;

import back.fcz.domain.member.dto.request.MemberLoginRequest;
import back.fcz.domain.member.dto.request.MemberSignupRequest;
import back.fcz.domain.member.dto.response.LoginTokensResponse;
import back.fcz.domain.member.dto.response.MemberSignupResponse;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.service.PhoneVerificationService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.security.jwt.JwtProperties;
import back.fcz.global.security.jwt.JwtProvider;
import back.fcz.global.security.jwt.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private MemberRepository memberRepository;
    private PhoneCrypto phoneCrypto;
    private PasswordEncoder passwordEncoder;
    private JwtProvider jwtProvider;
    private AuthService authService;
    private RefreshTokenService refreshTokenService;
    private JwtProperties jwtProperties;
    private PhoneVerificationService phoneVerificationService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        phoneCrypto = mock(PhoneCrypto.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtProvider = mock(JwtProvider.class);
        refreshTokenService = mock(RefreshTokenService.class);
        jwtProperties = mock(JwtProperties.class);
        phoneVerificationService = mock(PhoneVerificationService.class);
        authService = new AuthService(memberRepository, phoneCrypto, passwordEncoder, jwtProvider, refreshTokenService, jwtProperties, phoneVerificationService);
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        MemberSignupRequest req = new MemberSignupRequest(
                "uid", "pw1234", "홍길동", "길동이", "01012345678"
        );

        // userId 중복 체크
        when(memberRepository.existsByUserIdAndDeletedAtIsNull("uid"))
                .thenReturn(false);
        when(memberRepository.findByUserIdAndDeletedAtIsNotNull("uid"))
                .thenReturn(Optional.empty());

        // nickname 중복
        when(memberRepository.existsByNickname("길동이"))
                .thenReturn(false);

        // phone 중복 체크
        when(phoneCrypto.hash("01012345678"))
                .thenReturn("HASHED");
        when(memberRepository.existsByPhoneHashAndDeletedAtIsNull("HASHED"))
                .thenReturn(false);
        when(memberRepository.findByPhoneHashAndDeletedAtIsNotNull("HASHED"))
                .thenReturn(Optional.empty());

        // 번호 인증
        when(phoneVerificationService.isPhoneVerified(
                "01012345678",
                PhoneVerificationPurpose.SIGNUP
        )).thenReturn(true);


        // 암호화
        when(phoneCrypto.encrypt("01012345678"))
                .thenReturn("ENCRYPTED");
        when(passwordEncoder.encode("pw1234"))
                .thenReturn("ENC_PW");

        Member saved = Member.create(
                "uid", "ENC_PW", "홍길동", "길동이", "ENCRYPTED", "HASHED"
        );
        ReflectionTestUtils.setField(saved, "memberId", 1L);

        when(memberRepository.save(any()))
                .thenReturn(saved);

        MemberSignupResponse res = authService.signup(req);

        assertEquals(1L, res.memberId());
        assertEquals("uid", res.userId());
    }


    @Test
    @DisplayName("회원가입 실패 - 중복 userId")
    void signup_duplicate_userId() {
        MemberSignupRequest req = new MemberSignupRequest(
                "uid", "pw", "홍길동", "닉", "010"
        );
        when(memberRepository.existsByUserIdAndDeletedAtIsNull("uid"))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.signup(req));
        assertEquals(ErrorCode.DUPLICATE_USER_ID, ex.getErrorCode());
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 nickname")
    void signup_duplicate_nickname() {
        MemberSignupRequest req = new MemberSignupRequest(
                "uid", "pw", "홍길동", "닉", "010"
        );
        when(memberRepository.existsByNickname("닉")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.signup(req));
        assertEquals(ErrorCode.DUPLICATE_NICKNAME, ex.getErrorCode());
    }

    @Test
    @DisplayName("회원가입 실패 - 탈퇴한 userId")
    void signup_withdrawn_userId() {
        MemberSignupRequest req = new MemberSignupRequest(
                "uid", "pw", "홍길동", "닉", "010"
        );

        when(memberRepository.existsByUserIdAndDeletedAtIsNull("uid"))
                .thenReturn(false);
        when(memberRepository.findByUserIdAndDeletedAtIsNotNull("uid"))
                .thenReturn(Optional.of(mock(Member.class)));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> authService.signup(req));

        assertEquals(ErrorCode.WITHDRAWN_USER_ID, ex.getErrorCode());
    }


    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        MemberLoginRequest req = new MemberLoginRequest("uid", "pw");

        Member mockMember = Member.create(
                "uid", "ENC_PW", "홍길동", "길동", "enc", "hash"
        );
        ReflectionTestUtils.setField(mockMember, "memberId", 1L);

        when(memberRepository.findByUserId("uid")).thenReturn(Optional.of(mockMember));
        when(passwordEncoder.matches("pw", "ENC_PW")).thenReturn(true);
        when(jwtProvider.generateMemberAccessToken(1L, MemberRole.USER.name()))
                .thenReturn("ATK");
        when(jwtProvider.generateMemberRefreshToken(1L, MemberRole.USER.name()))
                .thenReturn("RTK");

        JwtProperties.TokenConfig refreshTokenConfig = mock(JwtProperties.TokenConfig.class);
        when(jwtProperties.getRefreshToken()).thenReturn(refreshTokenConfig);
        when(refreshTokenConfig.getExpiration()).thenReturn(3600000L);

        doNothing().when(refreshTokenService)
                .saveMemberRefreshToken(anyLong(), anyString(), anyLong());

        LoginTokensResponse res = authService.login(req);

        assertEquals("ATK", res.accessToken());
        assertEquals("RTK", res.refreshToken());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_invalid_password() {
        MemberLoginRequest req = new MemberLoginRequest("uid", "pw");

        Member mockMember = Member.create("uid", "ENC_PW", "홍길동", "닉", "enc", "hash");
        when(memberRepository.findByUserId("uid")).thenReturn(Optional.of(mockMember));
        when(passwordEncoder.matches("pw", "ENC_PW")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals(ErrorCode.INVALID_PASSWORD, ex.getErrorCode());
    }
}
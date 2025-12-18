package back.fcz.domain.member.service;

import back.fcz.domain.member.dto.request.MemberUpdateRequest;
import back.fcz.domain.member.dto.request.PasswordVerifyRequest;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.member.repository.NicknameHistoryRepository;
import back.fcz.domain.sms.service.PhoneVerificationService;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("sms")
class MemberServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PhoneCrypto phoneCrypto;
    @Mock
    private NicknameHistoryRepository nicknameHistoryRepository;
    @Mock
    private PhoneVerificationService phoneVerificationService;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(
                passwordEncoder,
                memberRepository,
                phoneCrypto,
                nicknameHistoryRepository,
                phoneVerificationService
        );
    }

    private Member mockMember() {
        Member member = Member.create(
                "uid",
                "ENC_PW",
                "홍길동",
                "oldNick",
                "ENC_PHONE",
                "HASH"
        );
        ReflectionTestUtils.setField(member, "memberId", 1L);
        ReflectionTestUtils.setField(member, "role", MemberRole.USER);
        return member;
    }

    private InServerMemberResponse mockUser(Member member) {
        return InServerMemberResponse.from(member);
    }

    @Test
    @DisplayName("비밀번호 검증 성공")
    void verifyPassword_success() {
        Member member = mockMember();

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("pw", "ENC_PW"))
                .thenReturn(true);

        memberService.verifyPassword(
                mockUser(member),
                new PasswordVerifyRequest("pw")
        );
    }

    @Test
    @DisplayName("비밀번호 검증 실패")
    void verifyPassword_fail() {
        Member member = mockMember();

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("pw", "ENC_PW"))
                .thenReturn(false);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> memberService.verifyPassword(
                        mockUser(member),
                        new PasswordVerifyRequest("pw")
                )
        );

        assertEquals(ErrorCode.INVALID_PASSWORD, ex.getErrorCode());
    }

    @Test
    @DisplayName("회원 정보 조회 (마스킹)")
    void getMe_success() {
        Member member = mockMember();

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(phoneCrypto.decrypt("ENC_PHONE"))
                .thenReturn("01012345678");

        memberService.getMe(mockUser(member));

        verify(phoneCrypto).decrypt("ENC_PHONE");
    }

    @Test
    @DisplayName("닉네임 변경 성공")
    void updateNickname_success() {
        Member member = mockMember();
        ReflectionTestUtils.setField(
                member,
                "nicknameChangedAt",
                LocalDateTime.now().minusDays(100)
        );

        MemberUpdateRequest req =
                new MemberUpdateRequest("newNick", null, null, null);


        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("newNick"))
                .thenReturn(false);

        memberService.updateMember(mockUser(member), req);

        assertEquals("newNick", member.getNickname());
        verify(nicknameHistoryRepository).save(any());
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() {
        Member member = mockMember();

        MemberUpdateRequest req =
                new MemberUpdateRequest(null, "oldPw", "newPw", null);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("oldPw", "ENC_PW"))
                .thenReturn(true);
        when(passwordEncoder.encode("newPw"))
                .thenReturn("NEW_HASH");

        memberService.updateMember(mockUser(member), req);

        assertEquals("NEW_HASH", member.getPasswordHash());
    }
    
    // 임시 주석 처리
//    @Test
//    @DisplayName("전화번호 변경 성공")
//    void updatePhone_success() {
//        Member member = mockMember();
//
//        MemberUpdateRequest req =
//                new MemberUpdateRequest(null, null, null, "01099998888");
//
//        when(memberRepository.findById(1L))
//                .thenReturn(Optional.of(member));
//
//        // 번호 인증
//        when(phoneVerificationService.isPhoneVerified(
//                "01099998888",
//                PhoneVerificationPurpose.CHANGE_PHONE
//        )).thenReturn(true);
//
//        when(phoneCrypto.hash("01099998888"))
//                .thenReturn("NEW_HASH");
//        when(memberRepository.existsByPhoneHashAndMemberIdNot("NEW_HASH", 1L))
//                .thenReturn(false);
//        when(phoneCrypto.encrypt("01099998888"))
//                .thenReturn("ENC_NEW_PHONE");
//
//        memberService.updateMember(mockUser(member), req);
//
//        assertEquals("NEW_HASH", member.getPhoneHash());
//    }


    @Test
    @DisplayName("회원 탈퇴 성공")
    void delete_success() {
        Member member = mockMember();

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        memberService.delete(mockUser(member));

        assertNotNull(member.getDeletedAt());
        assertEquals(MemberStatus.EXIT, member.getStatus());
    }
}

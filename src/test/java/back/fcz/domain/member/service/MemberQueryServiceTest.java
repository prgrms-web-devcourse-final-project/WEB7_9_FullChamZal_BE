package back.fcz.domain.member.service;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberQueryServiceTest {

    private MemberRepository memberRepository;
    private PhoneCrypto phoneCrypto;
    private MemberQueryService memberQueryService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        phoneCrypto = mock(PhoneCrypto.class);
        memberQueryService = new MemberQueryService(memberRepository, phoneCrypto);
    }

    @Test
    @DisplayName("회원 ID로 사용자 조회 성공")
    void getMemberById_success() {
        Member m = Member.create("uid", "pw", "홍길동", "닉", "enc", "hash");
        ReflectionTestUtils.setField(m, "memberId", 1L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(m));

        InServerMemberResponse res = memberQueryService.getMemberById(1L);

        assertEquals("uid", res.userId());
    }

    @Test
    @DisplayName("회원 ID로 조회 실패 - 사용자 없음")
    void getMemberById_not_found() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> memberQueryService.getMemberById(1L));
    }

    @Test
    @DisplayName("userId로 회원 조회 성공")
    void findByUserId_success() {
        Member m = Member.create("uid", "pw", "홍길동", "닉", "enc", "hash");

        when(memberRepository.findByUserId("uid")).thenReturn(Optional.of(m));

        Optional<InServerMemberResponse> res = memberQueryService.findByUserId("uid");

        assertTrue(res.isPresent());
        assertEquals("uid", res.get().userId());
    }

    @Test
    @DisplayName("전화번호로 회원 조회 성공 - hash 매칭")
    void findByPhoneNumber_success() {
        when(phoneCrypto.hash("010")).thenReturn("HASHED");

        Member m = Member.create("uid", "pw", "홍길동", "닉", "enc", "HASHED");
        when(memberRepository.findByPhoneHash("HASHED")).thenReturn(Optional.of(m));

        Optional<InServerMemberResponse> res = memberQueryService.findByPhoneNumber("010");

        assertTrue(res.isPresent());
        assertEquals("uid", res.get().userId());
    }
}

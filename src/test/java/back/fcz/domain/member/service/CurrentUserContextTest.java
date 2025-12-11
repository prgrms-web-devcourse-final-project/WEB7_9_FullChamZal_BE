package back.fcz.domain.member.service;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CurrentUserContextTest {

    private MemberRepository memberRepository;
    private CurrentUserContext currentUserContext;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        currentUserContext = new CurrentUserContext(memberRepository);
    }

    @Test
    @DisplayName("현재 사용자 조회 성공")
    void getCurrentUser_success() {
        Member member = Member.create(
                "uid", "pw", "홍길동", "닉", "enc", "hash"
        );
        ReflectionTestUtils.setField(member, "memberId", 1L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        InServerMemberResponse res = currentUserContext.getCurrentUser(1L);
        assertEquals(1L, res.memberId());
        assertEquals("uid", res.userId());
    }

    @Test
    @DisplayName("사용자 비활성화 예외")
    void getCurrentUser_not_active() {
        Member member = Member.create(
                "uid", "pw", "홍길동", "닉", "enc", "hash"
        );
        ReflectionTestUtils.setField(member, "status", MemberStatus.STOP);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> currentUserContext.getCurrentUser(1L));

        assertEquals(ErrorCode.MEMBER_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("현재 사용자 조회 실패 - 존재하지 않음")
    void getCurrentUser_not_found() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> currentUserContext.getCurrentUser(1L));

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, ex.getErrorCode());
    }
}

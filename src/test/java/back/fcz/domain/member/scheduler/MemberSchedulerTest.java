package back.fcz.domain.member.scheduler;

import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.crypto.PhoneCrypto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

class MemberSchedulerTest {

    private MemberRepository memberRepository;
    private CapsuleRecipientRepository recipientRepository;
    private PhoneCrypto phoneCrypto;

    private MemberScheduler memberScheduler;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        recipientRepository = mock(CapsuleRecipientRepository.class);
        phoneCrypto = mock(PhoneCrypto.class);

        memberScheduler = new MemberScheduler(
                memberRepository,
                recipientRepository,
                phoneCrypto
        );
    }

    @Test
    @DisplayName("익명화 대상 회원 존재")
    void anonymize_deleted_members_success() {
        Member m1 = mock(Member.class);
        when(m1.getPhoneHash()).thenReturn("HASH1");

        when(memberRepository.findAllByStatusAndDeletedAtBefore(
                eq(MemberStatus.EXIT),
                any(LocalDateTime.class)
        )).thenReturn(List.of(m1));

        memberScheduler.analymizeDeletedMembers();

        verify(m1).anonymize();
        verify(recipientRepository).anonymizeByRecipientPhoneHash("HASH1");
    }

    @Test
    @DisplayName("익명화 대상 회원 없음")
    void anonymize_deleted_members_empty() {
        when(memberRepository.findAllByStatusAndDeletedAtBefore(
                eq(MemberStatus.EXIT),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        memberScheduler.analymizeDeletedMembers();

        verify(recipientRepository, never())
                .anonymizeByRecipientPhoneHash(any());
    }
}
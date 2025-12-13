package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleReceiveDashBoardResponseDto;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CapsuleDashBoardServiceTest {
    @InjectMocks
    private CapsuleDashBoardService capsuleDashBoardService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CapsuleRecipientRepository capsuleRecipientRepository;

    @Mock
    private Member testMember;
    private final long MEMBER_ID = 1L;
    private final String PHONE_HASH = "TEST_PHONE_HASH_1234";

    private Capsule capsule1;
    private Capsule capsule2;
    private CapsuleRecipient recipient1;
    private CapsuleRecipient recipient2;

    @BeforeEach
    void setUpForReceiveDashBoard() {
        // 테스트 member 설정
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(testMember));
        when(testMember.getPhoneHash()).thenReturn(PHONE_HASH);

        // 테스트 capsule 설정
        capsule1 = Capsule.builder()
                .capsuleId(1L)
                .uuid("test")
                .nickname("test")
                .content("test")
                .capsuleColor("RED")
                .capsulePackingColor("RED")
                .visibility("PRIVATE")
                .unlockType("TIME")
                .build();

        capsule2 = Capsule.builder()
                .capsuleId(2L)
                .uuid("test")
                .nickname("test")
                .content("test")
                .capsuleColor("RED")
                .capsulePackingColor("RED")
                .visibility("PUBLIC")
                .unlockType("TIME")
                .build();

        // 테스트 recipient 설정
        recipient1 = CapsuleRecipient.builder()
                .id(1L)
                .capsuleId(capsule1)
                .recipientPhoneHash(PHONE_HASH)
                .build();

        recipient2 = CapsuleRecipient.builder()
                .id(2L)
                .capsuleId(capsule2)
                .recipientPhoneHash(PHONE_HASH)
                .build();
    }

    // ------------------------------------------
    // 수신한 캡슐 리스트 조회 (readReceiveCapsuleList)
    // ------------------------------------------

    @Test
    @DisplayName("멤버의 해시된 전화번호로 수신자 목록을 조회한 뒤, 수신자 목록에서 수신 캡슐 목록을 조회한다")
    void readReceiveCapsuleList_Success() {
        // given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(testMember));

        List<CapsuleRecipient> recipients = Arrays.asList(recipient1, recipient2);
        when(capsuleRecipientRepository.findAllByRecipientPhoneHashWithCapsule(PHONE_HASH))
                .thenReturn(recipients);

        // When
        List<CapsuleReceiveDashBoardResponseDto> result = capsuleDashBoardService.readReceiveCapsuleList(MEMBER_ID);

        // Then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).capsuleId()).isEqualTo(1L);
        assertThat(result.get(1).capsuleId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("수신된 캡슐이 없는 경우, 빈 리스트를 반환한다")
    void readReceiveCapsuleList_NoCapsules_ReturnsEmptyList() {
        // given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(testMember));
        when(capsuleRecipientRepository.findAllByRecipientPhoneHashWithCapsule(PHONE_HASH))
                .thenReturn(List.of());  // 빈 리스트 반환

        // when
        List<CapsuleReceiveDashBoardResponseDto> result = capsuleDashBoardService.readReceiveCapsuleList(MEMBER_ID);

        // then
        assertThat(result).isEmpty();
    }
}

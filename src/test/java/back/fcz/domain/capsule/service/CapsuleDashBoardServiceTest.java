package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.global.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CapsuleDashBoardServiceTest {
    @InjectMocks
    private CapsuleDashBoardService capsuleDashBoardService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CapsuleRecipientRepository capsuleRecipientRepository;

    @Mock
    private CapsuleRepository capsuleRepository;

    @Mock
    private Member testMember;
    private final long MEMBER_ID = 1L;
    private final String PHONE_HASH = "TEST_PHONE_HASH_1234";

    private Capsule capsule1;
    private Capsule capsule2;
    private CapsuleRecipient recipient1;
    private CapsuleRecipient recipient2;

    @BeforeEach
    void setUp() {
        // 테스트 capsule 설정
        capsule1 = Capsule.builder()
                .capsuleId(1L)
                .uuid("test")
                .nickname("test")
                .receiverNickname("receiver test")
                .content("test")
                .capsuleColor("RED")
                .capsulePackingColor("RED")
                .visibility("PRIVATE")
                .unlockType("TIME")
                .isDeleted(0)
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
                .isDeleted(0)
                .build();

        // 테스트 recipient 설정
        recipient1 = CapsuleRecipient.builder()
                .id(1L)
                .capsuleId(capsule1)
                .recipientName("recipient1")
                .recipientPhone("test")
                .recipientPhoneHash(PHONE_HASH)
                .isSenderSelf(1)
                .build();

        recipient2 = CapsuleRecipient.builder()
                .id(2L)
                .capsuleId(capsule2)
                .recipientName("recipient2")
                .recipientPhone("test")
                .recipientPhoneHash(PHONE_HASH)
                .isSenderSelf(1)
                .build();
    }

    void setUpForReceiveDashBoard() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(testMember));
        when(testMember.getPhoneHash()).thenReturn(PHONE_HASH);
    }

    // ------------------------------------------
    // 전송한 캡슐 리스트 조회 (readSendCapsuleList)
    // ------------------------------------------

    @Test
    @DisplayName("전송한 캡슐 목록과 해당 캡슐에 대한 수신자를 조회한다")
    void readSendCapsuleList_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Capsule> sendCapsules = Arrays.asList(capsule1, capsule2);
        // List를 Page 객체로 감싸서 반환하도록 수정
        Page<Capsule> capsulePage = new PageImpl<>(sendCapsules, pageable, sendCapsules.size());

        // findActiveCapsulesByMemberId에 pageable 파라미터 추가
        when(capsuleRepository.findActiveCapsulesByMemberId(MEMBER_ID, pageable))
                .thenReturn(capsulePage);

        // when
        // 서비스 메서드 호출 시 pageable 전달, 반환 타입은 Page로 변경
        Page<CapsuleDashBoardResponse> result = capsuleDashBoardService.readSendCapsuleList(MEMBER_ID, pageable);

        // then
        assertThat(result.getContent()).hasSize(2); // Page의 내용을 검증할 땐 getContent() 사용
        assertThat(result.getContent().get(0).capsuleId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("전송한 캡슐이 없는 경우, 빈 페이지를 반환한다")
    void readSendCapsuleList_no_capsules() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        when(capsuleRepository.findActiveCapsulesByMemberId(MEMBER_ID, pageable))
                .thenReturn(Page.empty(pageable));

        // when
        Page<CapsuleDashBoardResponse> result = capsuleDashBoardService.readSendCapsuleList(MEMBER_ID, pageable);

        // then
        assertThat(result).isEmpty();
    }

    // ------------------------------------------
    // 수신한 캡슐 리스트 조회 (readReceiveCapsuleList)
    // ------------------------------------------

    @Test
    @DisplayName("멤버의 해시된 전화번호로 수신자 목록을 조회한 뒤, 수신자 목록에서 수신 캡슐 목록을 조회한다")
    void readReceiveCapsuleList_Success() {
        // given
        setUpForReceiveDashBoard();
        Pageable pageable = PageRequest.of(0, 10);
        List<CapsuleRecipient> recipients = Arrays.asList(recipient1, recipient2);
        Page<CapsuleRecipient> recipientPage = new PageImpl<>(recipients, pageable, recipients.size());

        when(capsuleRecipientRepository.findAllByRecipientPhoneHashWithCapsule(PHONE_HASH, pageable))
                .thenReturn(recipientPage);

        // When
        Page<CapsuleDashBoardResponse> result = capsuleDashBoardService.readReceiveCapsuleList(MEMBER_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).capsuleId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("수신된 캡슐이 없는 경우, 빈 페이지를 반환한다")
    void readReceiveCapsuleList_no_capsules() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        setUpForReceiveDashBoard();
        when(capsuleRecipientRepository.findAllByRecipientPhoneHashWithCapsule(PHONE_HASH, pageable))
             .thenReturn(Page.empty(pageable));  // 빈 페이지 반환

        // when
        Page<CapsuleDashBoardResponse> result = capsuleDashBoardService.readReceiveCapsuleList(MEMBER_ID, pageable);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("스토리트랙용 공개 장소 기반 캡슐 조회 성공")
    void myPublicLocationCapsule_success() {
        // given
        Long memberId = 1L;
        int page = 0;
        int size = 2;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "capsuleId")
        );

        Capsule capsule1 = Capsule.builder()
                .capsuleId(1L)
                .title("캡슐1")
                .unlockType("LOCATION")
                .build();

        Capsule capsule2 = Capsule.builder()
                .capsuleId(2L)
                .title("캡슐2")
                .unlockType("TIME_AND_LOCATION")
                .build();

        Page<Capsule> capsulePage =
                new PageImpl<>(List.of(capsule1, capsule2), pageable, 2);

        given(capsuleRepository.findMyCapsulesLocationType(
                eq(memberId),
                eq("PUBLIC"),
                eq(List.of("LOCATION", "TIME_AND_LOCATION")),
                any(Pageable.class)
        )).willReturn(capsulePage);

        // when
        PageResponse<CapsuleDashBoardResponse> response =
                capsuleDashBoardService.myPublicLocationCapsule(memberId, page, size);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);

        CapsuleDashBoardResponse dto1 = response.getContent().get(0);
        assertThat(dto1.capsuleId()).isEqualTo(1L);
        assertThat(dto1.unlockType()).isEqualTo("LOCATION");

        CapsuleDashBoardResponse dto2 = response.getContent().get(1);
        assertThat(dto2.capsuleId()).isEqualTo(2L);
        assertThat(dto2.unlockType()).isEqualTo("TIME_AND_LOCATION");

        verify(capsuleRepository, times(1))
                .findMyCapsulesLocationType(
                        eq(memberId),
                        eq("PUBLIC"),
                        eq(List.of("LOCATION", "TIME_AND_LOCATION")),
                        any(Pageable.class)
                );
    }

}

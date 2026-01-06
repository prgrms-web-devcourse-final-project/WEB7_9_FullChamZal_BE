package back.fcz.domain.storytrack.service;

import back.fcz.domain.capsule.DTO.request.CapsuleConditionRequestDTO;
import back.fcz.domain.capsule.DTO.response.CapsuleConditionResponseDTO;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.capsule.service.CapsuleReadService;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.storytrack.dto.request.CreateStorytrackRequest;
import back.fcz.domain.storytrack.dto.request.JoinStorytrackRequest;
import back.fcz.domain.storytrack.dto.response.CreateStorytrackResponse;
import back.fcz.domain.storytrack.dto.response.JoinStorytrackResponse;
import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.domain.storytrack.entity.StorytrackProgress;
import back.fcz.domain.storytrack.entity.StorytrackStep;
import back.fcz.domain.storytrack.repository.StorytrackAttachmentRepository;
import back.fcz.domain.storytrack.repository.StorytrackProgressRepository;
import back.fcz.domain.storytrack.repository.StorytrackRepository;
import back.fcz.domain.storytrack.repository.StorytrackStepRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorytrackServiceTest {

    @InjectMocks
    private StorytrackService storytrackService;

    @Mock private StorytrackRepository storytrackRepository;
    @Mock private StorytrackProgressRepository storytrackProgressRepository;
    @Mock private StorytrackStepRepository storytrackStepRepository;
    @Mock private CapsuleRepository capsuleRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CapsuleReadService capsuleReadService;
    @Mock private StorytrackAttachmentRepository storytrackAttachmentRepository;

    @Test
    @DisplayName("스토리트랙 생성 성공")
    void createStorytrack_success() {
        // given
        Long memberId = 1L;

        Member member = Member.builder()
                .build();

        Capsule capsule = Capsule.builder()
                .capsuleId(10L)
                .visibility("PUBLIC")
                .build();

        CreateStorytrackRequest request = new CreateStorytrackRequest(
                "title",
                "desc",
                "SEQUENTIAL",
                1,
                0,
                List.of(10L),
                null
        );

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));

        given(capsuleRepository.findById(10L))
                .willReturn(Optional.of(capsule));

        given(storytrackRepository.save(any()))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        CreateStorytrackResponse response =
                storytrackService.createStorytrack(request, memberId);

        // then
        assertThat(response.title()).isEqualTo("title");
        assertThat(response.totalSteps()).isEqualTo(1);
        assertThat(response.capsuleList()).containsExactly(10L);

        verify(storytrackAttachmentRepository, never()).save(any());
        verify(storytrackAttachmentRepository,never()).saveAll(any());

    }

    @Test
    @DisplayName("스토리트랙 생성 실패 - 비공개 캡슐")
    void createStorytrack_fail_privateCapsule() {
        Long memberId = 1L;

        Member member = Member.builder().build();

        Capsule capsule = Capsule.builder()
                .capsuleId(10L)
                .visibility("PRIVATE")
                .build();

        CreateStorytrackRequest request = new CreateStorytrackRequest(
                "title",
                "desc",
                "SEQUENTIAL",
                1,
                0,
                List.of(10L),
                null
        );

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));

        given(capsuleRepository.findById(10L))
                .willReturn(Optional.of(capsule));

        // when & then
        assertThatThrownBy(() ->
                storytrackService.createStorytrack(request, memberId)
        ).isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CAPSULE_NOT_PUBLIC.getMessage());
    }

    @Test
    @DisplayName("스토리트랙 생성 시 totalSteps는 capsuleList 크기와 동일")
    void createStorytrack_totalSteps_matchCapsuleCount() {
        Long memberId = 1L;

        Member member = Member.builder().build();

        Capsule capsule1 = Capsule.builder()
                .capsuleId(1L)
                .visibility("PUBLIC")
                .build();

        Capsule capsule2 = Capsule.builder()
                .capsuleId(2L)
                .visibility("PUBLIC")
                .build();

        CreateStorytrackRequest request = new CreateStorytrackRequest(
                "title",
                "desc",
                "SEQUENTIAL",
                1,
                0,
                List.of(1L, 2L),
                null
        );

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));

        given(capsuleRepository.findById(1L))
                .willReturn(Optional.of(capsule1));

        given(capsuleRepository.findById(2L))
                .willReturn(Optional.of(capsule2));

        given(storytrackRepository.save(any()))
                .willAnswer(inv -> inv.getArgument(0));

        CreateStorytrackResponse response =
                storytrackService.createStorytrack(request, memberId);

        assertThat(response.totalSteps()).isEqualTo(2);
    }

    @Test
    @DisplayName("스토리트랙 생성 시 capsuleList 순서가 stepOrder 순서가 된다")
    void createStorytrack_stepOrder_matchCapsuleOrder() {
        Long memberId = 1L;

        Member member = Member.builder().build();

        Capsule c1 = Capsule.builder().capsuleId(1L).visibility("PUBLIC").build();
        Capsule c2 = Capsule.builder().capsuleId(2L).visibility("PUBLIC").build();

        CreateStorytrackRequest request = new CreateStorytrackRequest(
                "title",
                "desc",
                "SEQUENTIAL",
                1,
                0,
                List.of(2L, 1L),
                null
        );

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(capsuleRepository.findById(2L)).willReturn(Optional.of(c2));
        given(capsuleRepository.findById(1L)).willReturn(Optional.of(c1));

        given(storytrackRepository.save(any()))
                .willAnswer(inv -> inv.getArgument(0));

        CreateStorytrackResponse response =
                storytrackService.createStorytrack(request, memberId);

        assertThat(response.capsuleList()).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("스토리트랙 참여 성공")
    void joinStorytrack_success() {
        // given
        Long memberId = 1L;
        Long storytrackId = 100L;

        // 스토리트랙 생성자 (실제 객체)
        Member creator = Member.builder()
                .nickname("생성자닉네임")
                .build();

        // 참여자 (로그인 유저)
        Member member = Member.builder()
                .build();

        Storytrack storytrack = Storytrack.builder()
                .member(creator)
                .title("title")
                .description("desc")
                .trackType("SEQUENTIAL")
                .price(0)
                .totalSteps(3)
                .isPublic(1)
                .build();

        JoinStorytrackRequest request =
                new JoinStorytrackRequest(storytrackId);

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));

        given(storytrackRepository.findById(storytrackId))
                .willReturn(Optional.of(storytrack));

        // when
        JoinStorytrackResponse response =
                storytrackService.joinParticipant(request, memberId);

        // then
        assertThat(response.title()).isEqualTo("title");
        assertThat(response.storytrackType()).isEqualTo("SEQUENTIAL");
        assertThat(response.completedSteps()).isEqualTo(0);
    }

    @Test
    @DisplayName("이미 참여한 스토리트랙은 재참여 불가")
    void joinStorytrack_fail_alreadyJoined() {
        Long memberId = 1L;
        Long storytrackId = 10L;

        Member creator = Member.builder()
                .nickname("생성자닉네임")
                .build();
        ReflectionTestUtils.setField(creator, "memberId", 2L);

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Storytrack storytrack = Storytrack.builder()
                .member(creator)
                .isPublic(1)
                .build();

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));

        given(storytrackRepository.findById(storytrackId))
                .willReturn(Optional.of(storytrack));

        given(storytrackProgressRepository
                .existsByMember_MemberIdAndStorytrack_StorytrackIdAndDeletedAt(
                        memberId, storytrackId, null))
                .willReturn(true);

        assertThatThrownBy(() ->
                storytrackService.joinParticipant(
                        new JoinStorytrackRequest(storytrackId),
                        memberId
                )
        ).isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PARTICIPANT_ALREADY_JOIN.getMessage());
    }

    @Test
    @DisplayName("참여자 검증 실패")
    void validateParticipant_fail() {
        // given
        given(storytrackProgressRepository
                .existsByMember_MemberIdAndStorytrack_StorytrackId(1L, 10L))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                storytrackService.validateParticipant(1L, 10L)
        ).isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PARTICIPANT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("FREE 타입은 단계 검증 없이 통과")
    void validateStepAccess_freeType() {
        // given
        Storytrack storytrack = Storytrack.builder()
                .trackType("FREE")
                .build();

        StorytrackProgress progress = StorytrackProgress.builder()
                .storytrack(storytrack)
                .build();

        given(storytrackProgressRepository
                .findByStorytrack_StorytrackIdAndMember_MemberIdAndDeletedAtIsNull(10L, 1L))
                .willReturn(Optional.of(progress));

        // when & then
        assertThatCode(() ->
                storytrackService.validateStepAccess(1L, 10L, 100L)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("순차 스토리트랙 단계 오류")
    void validateStepAccess_invalidOrder() {
        // given
        Storytrack storytrack = Storytrack.builder()
                .storytrackId(10L)
                .trackType("SEQUENTIAL")
                .build();

        StorytrackProgress progress = StorytrackProgress.builder()
                .storytrack(storytrack)
                .lastCompletedStep(1)
                .completedAt(null)
                .build();

        StorytrackStep step = StorytrackStep.builder()
                .stepOrder(3)
                .build();

        given(storytrackProgressRepository
                .findByStorytrack_StorytrackIdAndMember_MemberIdAndDeletedAtIsNull(10L, 1L))
                .willReturn(Optional.of(progress));

        given(storytrackStepRepository
                .findByCapsule_CapsuleIdAndStorytrack_StorytrackId(100L, 10L))
                .willReturn(Optional.of(step));

        // when & then
        assertThatThrownBy(() ->
                storytrackService.validateStepAccess(1L, 10L, 100L)
        ).isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_STEP_ORDER.getMessage());

        verify(storytrackProgressRepository, times(1))
                .findByStorytrack_StorytrackIdAndMember_MemberIdAndDeletedAtIsNull(
                        10L, 1L
                );
    }

    @Test
    @DisplayName("캡슐 오픈 시 진행도 업데이트")
    void openCapsuleAndUpdateProgress_success() {
        // given
        Storytrack storytrack = Storytrack.builder()
                .totalSteps(3)
                .trackType("SEQUENTIAL")
                .build();

        StorytrackProgress progress = spy(
                StorytrackProgress.builder()
                        .storytrack(storytrack)
                        .completedSteps(0)
                        .lastCompletedStep(0)
                        .build()
        );

        StorytrackStep step = StorytrackStep.builder()
                .stepOrder(1)
                .build();

        CapsuleConditionRequestDTO request =
                new CapsuleConditionRequestDTO(
                        10L, null, null, null, null, null, null, null
                );

        CapsuleConditionResponseDTO response = mock(CapsuleConditionResponseDTO.class);
        given(response.result()).willReturn("SUCCESS");

        given(storytrackProgressRepository
                .findByStorytrack_StorytrackIdAndMember_MemberIdAndDeletedAtIsNull(1L, 1L))
                .willReturn(Optional.of(progress));

        given(storytrackStepRepository
                .findByCapsule_CapsuleIdAndStorytrack_StorytrackId(10L, 1L))
                .willReturn(Optional.of(step));

        given(capsuleReadService.conditionAndRead(request))
                .willReturn(response);

        // when
        storytrackService.openCapsuleAndUpdateProgress(1L, 1L, request);

        // then
        verify(progress).completeStep(step, 3);
    }
}

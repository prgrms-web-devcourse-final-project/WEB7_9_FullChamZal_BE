package back.fcz.domain.storytrack.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.storytrack.dto.request.UpdatePathRequest;
import back.fcz.domain.storytrack.dto.response.DeleteParticipantResponse;
import back.fcz.domain.storytrack.dto.response.DeleteStorytrackResponse;
import back.fcz.domain.storytrack.dto.response.UpdatePathResponse;
import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.domain.storytrack.entity.StorytrackProgress;
import back.fcz.domain.storytrack.entity.StorytrackStep;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StorytrackServieTest {
    @InjectMocks
    private StorytrackService storytrackService;

    @Mock
    private StorytrackRepository storytrackRepository;

    @Mock
    private StorytrackProgressRepository storytrackProgressRepository;

    @Mock
    private StorytrackStepRepository storytrackStepRepository;

    @Mock
    private CapsuleRepository capsuleRepository;

    // 삭제 서비스 테스트
    @Test
    @DisplayName("스토리트랙 생성자가 삭제 성공")
    void deleteStorytrack_success() {
        // given
        Long memberId = 1L;
        Long storytrackId = 10L;

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Storytrack storytrack = Storytrack.builder()
                .storytrackId(storytrackId)
                .member(member)
                .build();

        StorytrackStep step1 = mock(StorytrackStep.class);
        StorytrackStep step2 = mock(StorytrackStep.class);

        given(storytrackRepository.findById(storytrackId))
                .willReturn(Optional.of(storytrack));

        given(storytrackProgressRepository.countActiveParticipants(storytrackId))
                .willReturn(0L);

        given(storytrackStepRepository.findAllByStorytrack_StorytrackId(storytrackId))
                .willReturn(List.of(step1, step2));

        // when
        DeleteStorytrackResponse response =
                storytrackService.deleteStorytrack(memberId, storytrackId);

        // then
        assertThat(response.storytrackId()).isEqualTo(storytrackId);
        verify(storytrackRepository).save(storytrack);
        verify(step1).markDeleted();
        verify(step2).markDeleted();
    }

    @Test
    @DisplayName("스토리트랙이 없으면 예외 발생")
    void deleteStorytrack_notFound() {
        // given
        given(storytrackRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> storytrackService.deleteStorytrack(1L, 1L)
        );

        assertThat(ex.getErrorCode())
                .isEqualTo(ErrorCode.STORYTRACK_NOT_FOUND);
    }

    @Test
    @DisplayName("스토리트랙 생성자가 아니면 삭제 불가")
    void deleteStorytrack_notCreator() {
        // given
        Member creator = Member.builder().build();
        ReflectionTestUtils.setField(creator, "memberId", 1L);

        Storytrack storytrack = Storytrack.builder()
                .storytrackId(10L)
                .member(creator)
                .build();

        given(storytrackRepository.findById(10L))
                .willReturn(Optional.of(storytrack));

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> storytrackService.deleteStorytrack(2L, 10L)
        );

        assertThat(ex.getErrorCode())
                .isEqualTo(ErrorCode.NOT_STORYTRACK_CREATER);
    }

    @Test
    @DisplayName("참여자가 존재하면 삭제 불가")
    void deleteStorytrack_participantExists() {
        // given
        Long memberId = 1L;
        Long storytrackId = 10L;

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Storytrack storytrack = Storytrack.builder()
                .storytrackId(storytrackId)
                .member(member)
                .build();

        given(storytrackRepository.findById(storytrackId))
                .willReturn(Optional.of(storytrack));

        given(storytrackProgressRepository.countActiveParticipants(storytrackId))
                .willReturn(2L);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> storytrackService.deleteStorytrack(memberId, storytrackId)
        );

        assertThat(ex.getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPANT_EXISTS);
    }

    @Test
    @DisplayName("스토리트랙 참여자 삭제 성공")
    void deleteParticipant_success() {
        // given
        Long memberId = 1L;
        Long storytrackId = 10L;

        StorytrackProgress progress = mock(StorytrackProgress.class);

        given(storytrackProgressRepository
                .findByMember_MemberIdAndStorytrack_StorytrackId(memberId, storytrackId))
                .willReturn(Optional.of(progress));

        // when
        DeleteParticipantResponse response =
                storytrackService.deleteParticipant(memberId, storytrackId);

        // then
        verify(progress).markDeleted();
        verify(storytrackProgressRepository).save(progress);
        assertThat(response.message())
                .isEqualTo("스토리트랙 참여를 종료했습니다.");
    }

    @Test
    @DisplayName("참여자가 없으면 예외 발생")
    void deleteParticipant_notFound() {
        // given
        given(storytrackProgressRepository
                .findByMember_MemberIdAndStorytrack_StorytrackId(anyLong(), anyLong()))
                .willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> storytrackService.deleteParticipant(1L, 1L)
        );

        assertThat(ex.getErrorCode())
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    // 수정 서비스 테스트
    @Test
    @DisplayName("스토리트랙 경로 수정 성공")
    void updatePath_success() {
        // given
        Long stepId = 1L;
        Long loginMemberId = 10L;
        Long newCapsuleId = 100L;

        UpdatePathRequest request = new UpdatePathRequest(1, newCapsuleId);

        Member creator = Member.builder().build();
        ReflectionTestUtils.setField(creator, "memberId", loginMemberId);

        Storytrack storytrack = Storytrack.builder()
                .member(creator)
                .build();

        Capsule oldCapsule = Capsule.builder()
                .capsuleId(1L)
                .build();

        Capsule newCapsule = Capsule.builder()
                .capsuleId(newCapsuleId)
                .build();

        StorytrackStep step = StorytrackStep.builder()
                .storytrack(storytrack)
                .capsule(oldCapsule)
                .build();

        given(storytrackStepRepository.findById(stepId))
                .willReturn(Optional.of(step));

        given(capsuleRepository.findById(newCapsuleId))
                .willReturn(Optional.of(newCapsule));

        // when
        UpdatePathResponse response =
                storytrackService.updatePath(request, stepId, loginMemberId);

        // then
        assertThat(step.getCapsule()).isEqualTo(newCapsule);
        verify(storytrackStepRepository).save(step);
    }

    @Test
    @DisplayName("스토리트랙 경로가 없으면 수정 실패")
    void updatePath_stepNotFound() {
        // given
        Long stepId = 1L;
        UpdatePathRequest request = new UpdatePathRequest(1, 10L);

        given(storytrackStepRepository.findById(stepId))
                .willReturn(Optional.empty());

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> storytrackService.updatePath(request, stepId, 1L)
        );

        assertThat(ex.getErrorCode())
                .isEqualTo(ErrorCode.STORYTRACK_PAHT_NOT_FOUND);
    }

    @Test
    @DisplayName("스토리트랙 작성자가 아니면 경로 수정 실패")
    void updatePath_notCreator() {
        // given
        Long stepId = 1L;
        Long creatorId = 1L;
        Long loginMemberId = 99L; // ✅ 작성자와 다르게!
        Long newCapsuleId = 100L;

        UpdatePathRequest request = new UpdatePathRequest(1, newCapsuleId);

        Member creator = Member.builder().build();
        ReflectionTestUtils.setField(creator, "memberId", creatorId);

        Storytrack storytrack = Storytrack.builder().member(creator).build();

        StorytrackStep step = StorytrackStep.builder()
                .storytrack(storytrack)
                .build();

        given(storytrackStepRepository.findById(stepId))
                .willReturn(Optional.of(step));

        // when
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> storytrackService.updatePath(request, stepId, loginMemberId)
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_STORYTRACK_CREATER);

        verify(capsuleRepository, never()).findById(anyLong());
        verify(storytrackStepRepository, never()).save(any(StorytrackStep.class));
    }


    @Test
    @DisplayName("수정할 캡슐이 없으면 경로 수정 실패")
    void updatePath_capsuleNotFound() {
        // given
        Long stepId = 1L;
        Long loginMemberId = 1L;
        Long newCapsuleId = 99L;

        Member creator = Member.builder().build();
        ReflectionTestUtils.setField(creator, "memberId", loginMemberId);

        Storytrack storytrack = Storytrack.builder()
                .member(creator)
                .build();

        StorytrackStep step = StorytrackStep.builder()
                .storytrack(storytrack)
                .build();

        given(storytrackStepRepository.findById(stepId))
                .willReturn(Optional.of(step));

        given(capsuleRepository.findById(newCapsuleId))
                .willReturn(Optional.empty());

        UpdatePathRequest request = new UpdatePathRequest(1, newCapsuleId);

        // when & then
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> storytrackService.updatePath(request, stepId, loginMemberId)
        );

        assertThat(ex.getErrorCode())
                .isEqualTo(ErrorCode.CAPSULE_NOT_FOUND);
    }

}

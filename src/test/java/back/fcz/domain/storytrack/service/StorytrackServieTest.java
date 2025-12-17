package back.fcz.domain.storytrack.service;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.storytrack.dto.response.DeleteParticipantResponse;
import back.fcz.domain.storytrack.dto.response.DeleteStorytrackResponse;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}

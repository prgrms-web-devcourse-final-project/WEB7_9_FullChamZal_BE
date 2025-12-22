package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.domain.storytrack.entity.StorytrackProgress;

import java.time.LocalDateTime;

public record JoinStorytrackResponse(
        String title, // 스토리트랙 제목
        String description,
        String storytrackType,
        int price,
        int totalSteps,
        String nickname,
        int completedSteps,
        int lastCompletedStep,
        LocalDateTime startedAt
){
    public static JoinStorytrackResponse from(Storytrack storytrack, StorytrackProgress storytrackProgress){
        return new JoinStorytrackResponse(
                storytrack.getTitle(),
                storytrack.getDescription(),
                storytrack.getTrackType(),
                storytrack.getPrice(),
                storytrack.getTotalSteps(),
                storytrack.getMember().getNickname(),
                storytrackProgress.getCompletedSteps(),
                storytrackProgress.getLastCompletedStep(),
                storytrackProgress.getStartedAt()
        );
    }
}

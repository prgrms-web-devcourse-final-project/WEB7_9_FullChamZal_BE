package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.domain.storytrack.entity.StorytrackProgress;

import java.time.LocalDateTime;

public record ParticipantStorytrackListResponse(
        Long memberId,
        Long storytrackId,
        String title,
        String description,
        String trackType,
        int isPublic,
        int price,
        int totalSteps,
        int completedSteps,
        int lastCompletedStep,
        LocalDateTime startedAt,
        LocalDateTime completedAt
){
    public static ParticipantStorytrackListResponse from(StorytrackProgress progress, Storytrack storytrack){
        return new ParticipantStorytrackListResponse(
                storytrack.getMember().getMemberId(),
                storytrack.getStorytrackId(),
                storytrack.getTitle(),
                storytrack.getDescription(),
                storytrack.getTrackType(),
                storytrack.getIsPublic(),
                storytrack.getPrice(),
                storytrack.getTotalSteps(),
                progress.getCompletedSteps(),
                progress.getLastCompletedStep(),
                progress.getStartedAt(),
                progress.getCompletedAt()
        );
    }
}

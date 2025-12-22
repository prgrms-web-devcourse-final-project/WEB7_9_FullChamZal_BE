package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.storytrack.entity.StorytrackProgress;

import java.time.LocalDateTime;

public record ParticipantProgressResponse(
        Long storytrackId,
        int completedSteps,
        int lastCompletedStep,
        LocalDateTime completedAt,
        LocalDateTime createdAt
){
    public static ParticipantProgressResponse from(StorytrackProgress progress){
        return new ParticipantProgressResponse(
                progress.getStorytrack().getStorytrackId(),
                progress.getCompletedSteps(),
                progress.getLastCompletedStep(),
                progress.getCompletedAt(),
                progress.getCreatedAt()
        );
    }
}

package back.fcz.domain.storytrack.dto.response;

import java.time.LocalDateTime;

public record ParticipantProgressResponse(
        Long storytrackId,
        int completedSteps,
        int lastCompletedStep,
        LocalDateTime completedAt,
        LocalDateTime createdAt
){
}

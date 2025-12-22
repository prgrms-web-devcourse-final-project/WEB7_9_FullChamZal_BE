package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.storytrack.dto.PathResponse;
import back.fcz.domain.storytrack.entity.Storytrack;

import java.time.LocalDateTime;
import java.util.List;

public record StorytrackDashBoardResponse(
        Long storytrackId,
        String title,
        String storytrackType,
        int isPublic,
        int totalSteps,
        LocalDateTime createdAt,
        int totalParticipant,
        int completeParticipant,
        List<PathResponse> paths
){
    public static StorytrackDashBoardResponse of(
            Storytrack storytrack,
            List<PathResponse> paths,
            int totalParticipant,
            int completeProgress
    ) {
        return new StorytrackDashBoardResponse(
                storytrack.getStorytrackId(),
                storytrack.getTitle(),
                storytrack.getTrackType(),
                storytrack.getIsPublic(),
                storytrack.getTotalSteps(),
                storytrack.getCreatedAt(),

                totalParticipant,
                completeProgress,

                paths
        );
    }
}

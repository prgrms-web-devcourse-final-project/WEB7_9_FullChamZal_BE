package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.storytrack.dto.PathResponse;
import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.global.dto.PageResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public record StorytrackDashBoardResponse(
        Long storytrackId,
        String createrNickname,
        String title,
        String storytrackType,
        int isPublic,
        int totalSteps,
        LocalDateTime createdAt,
        int totalParticipant,
        int completeParticipant,
        PageResponse<PathResponse> paths
){
    public static StorytrackDashBoardResponse of(
            Storytrack storytrack,
            Page<PathResponse> paths,
            int totalParticipant,
            int completeProgress
    ) {
        return new StorytrackDashBoardResponse(
                storytrack.getStorytrackId(),
                storytrack.getMember().getNickname(),
                storytrack.getTitle(),
                storytrack.getTrackType(),
                storytrack.getIsPublic(),
                storytrack.getTotalSteps(),
                storytrack.getCreatedAt(),

                totalParticipant,
                completeProgress,

                new PageResponse<> (paths)
        );
    }
}

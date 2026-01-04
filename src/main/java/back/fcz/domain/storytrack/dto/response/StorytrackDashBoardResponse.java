package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.storytrack.dto.PathResponse;
import back.fcz.domain.storytrack.dto.StorytrackMemberType;
import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.global.dto.PageResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public record StorytrackDashBoardResponse(
        Long storytrackId,
        String createrNickname,
        String title,
        String descripton,
        String storytrackType,
        int isPublic,
        int totalSteps,
        LocalDateTime createdAt,
        int totalParticipant,
        int completeParticipant,
        StorytrackMemberType memberType,
        PageResponse<PathResponse> paths,
        List<Long> completedCapsuleId,
        String imageUrl
){
    public static StorytrackDashBoardResponse of(
            Storytrack storytrack,
            Page<PathResponse> paths,
            int totalParticipant,
            int completeProgress,
            StorytrackMemberType memberType,
            List<Long> completedCapsuleId,
            String imageUrl
    ) {
        return new StorytrackDashBoardResponse(
                storytrack.getStorytrackId(),
                storytrack.getMember().getNickname(),
                storytrack.getTitle(),
                storytrack.getDescription(),
                storytrack.getTrackType(),
                storytrack.getIsPublic(),
                storytrack.getTotalSteps(),
                storytrack.getCreatedAt(),

                totalParticipant,
                completeProgress,

                memberType,

                new PageResponse<> (paths),

                completedCapsuleId,
                imageUrl
        );
    }
}

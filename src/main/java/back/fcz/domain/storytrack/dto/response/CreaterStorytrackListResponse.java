package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.storytrack.entity.Storytrack;

import java.time.LocalDateTime;

public record CreaterStorytrackListResponse(
        Long storytrackId,
        String title,
        String description,
        String trackType,
        int isPublic,
        int price,
        int totalSteps,
        LocalDateTime createdAt
){
    public static CreaterStorytrackListResponse from(Storytrack storytrack) {
        return new CreaterStorytrackListResponse(
                storytrack.getStorytrackId(),
                storytrack.getTitle(),
                storytrack.getDescription(),
                storytrack.getTrackType(),
                storytrack.getIsPublic(),
                storytrack.getPrice(),
                storytrack.getTotalSteps(),
                storytrack.getCreatedAt()
        );
    }
}

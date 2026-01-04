package back.fcz.domain.storytrack.dto.response;


import back.fcz.domain.storytrack.dto.StorytrackMemberType;

import java.time.LocalDateTime;

public record TotalStorytrackResponse(
        Long storytrackId,
        String createrName,
        String title,
        String description,
        String trackType,
        int isPublic,
        int price,
        int totalSteps,
        LocalDateTime createdAt,
        Long totalMemberCount,
        StorytrackMemberType memberType,
        String imageUrl
){
    public TotalStorytrackResponse withImage(String imageUrl) {
        return new TotalStorytrackResponse(
                storytrackId,
                createrName,
                title,
                description,
                trackType,
                isPublic,
                price,
                totalSteps,
                createdAt,
                totalMemberCount,
                memberType,
                imageUrl
        );
    }
}

package back.fcz.domain.storytrack.dto.response;

import java.time.LocalDateTime;

public record CreaterStorytrackListResponse(
        Long storytrackId,
        String creatorNickname,
        String title,
        String description,
        String trackType,
        int isPublic,
        int price,
        int totalSteps,
        LocalDateTime createdAt,
        Long totalMemberCount,
        String imageUrl
){
    public CreaterStorytrackListResponse withImageUrl(String imageUrl) {
        return new CreaterStorytrackListResponse(
                storytrackId,
                creatorNickname,
                title,
                description,
                trackType,
                isPublic,
                price,
                totalSteps,
                createdAt,
                totalMemberCount,
                imageUrl
        );
    }
}

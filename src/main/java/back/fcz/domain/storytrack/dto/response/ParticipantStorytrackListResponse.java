package back.fcz.domain.storytrack.dto.response;

import java.time.LocalDateTime;

public record ParticipantStorytrackListResponse(
        Long memberId,
        Long storytrackId,
        String creatorNickname,
        String title,
        String description,
        String trackType,
        int isPublic,
        int price,
        int totalSteps,
        int completedSteps,
        int lastCompletedStep,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        Long totalMemberCount,
        String imageUrl
){
    public ParticipantStorytrackListResponse withImageUrl(String imageUrl) {
        return new ParticipantStorytrackListResponse(
                memberId,
                storytrackId,
                creatorNickname,
                title,
                description,
                trackType,
                isPublic,
                price,
                totalSteps,
                completedSteps,
                lastCompletedStep,
                startedAt,
                completedAt,
                createdAt,
                totalMemberCount,
                imageUrl
        );
    }
}

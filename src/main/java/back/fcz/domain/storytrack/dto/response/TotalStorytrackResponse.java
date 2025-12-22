package back.fcz.domain.storytrack.dto.response;


import back.fcz.domain.storytrack.entity.Storytrack;

public record TotalStorytrackResponse(
        Long storytrackId,
        String createrName,
        String title,
        String desctiption,
        String trackType,
        int isPublic,
        int price,
        int totalSteps
){
    public static TotalStorytrackResponse from(Storytrack track){
        return new TotalStorytrackResponse(
                track.getStorytrackId(),
                track.getMember().getNickname(),
                track.getTitle(),
                track.getDescription(),
                track.getTrackType(),
                track.getIsPublic(),
                track.getPrice(),
                track.getTotalSteps()
        );
    }
}

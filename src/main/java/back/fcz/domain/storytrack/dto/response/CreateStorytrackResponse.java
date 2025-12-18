package back.fcz.domain.storytrack.dto.response;

import java.util.List;

public record CreateStorytrackResponse(
        Long storytrackId,
        String title,
        String description,
        String trackType,
        int isPublic,
        int price,
        int totalSteps,
        List<Integer> capsuleList
){
}

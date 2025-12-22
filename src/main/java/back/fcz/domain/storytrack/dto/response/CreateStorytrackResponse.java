package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.domain.storytrack.entity.StorytrackStep;

import java.util.Comparator;
import java.util.List;

public record CreateStorytrackResponse(
        Long storytrackId,
        String title,
        String description,
        String trackType,
        int isPublic,
        int price,
        int totalSteps,
        List<Long> capsuleList
){
    public static CreateStorytrackResponse from(Storytrack storytrack) {

        List<Long> capsuleIds = storytrack.getSteps().stream()
                .sorted(Comparator.comparingInt(StorytrackStep::getStepOrder)) // 순서 보장
                .map(step -> step.getCapsule().getCapsuleId())
                .toList();

        return new CreateStorytrackResponse(
                storytrack.getStorytrackId(),
                storytrack.getTitle(),
                storytrack.getDescription(),
                storytrack.getTrackType(),
                storytrack.getIsPublic(),
                storytrack.getPrice(),
                storytrack.getTotalSteps(),
                capsuleIds
        );
    }
}

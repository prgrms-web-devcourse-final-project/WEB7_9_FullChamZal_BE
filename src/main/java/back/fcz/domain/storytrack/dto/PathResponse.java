package back.fcz.domain.storytrack.dto;

import back.fcz.domain.storytrack.entity.StorytrackStep;

public record PathResponse(
        int stepOrder,
        CapsuleResponse capsule
) {
    public static PathResponse from(StorytrackStep step) {
        return new PathResponse(
                step.getStepOrder(),
                CapsuleResponse.from(step.getCapsule())
        );
    }
}
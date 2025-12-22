package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.capsule.DTO.GPSResponseDTO;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.storytrack.dto.CapsuleResponse;
import back.fcz.domain.storytrack.dto.UnlockResponse;
import back.fcz.domain.storytrack.entity.StorytrackStep;

public record UpdatePathResponse(
        Long storytrackId,
        int updatedStepOrder,
        CapsuleResponse capsule
) {
    public static UpdatePathResponse from(Capsule updateCapsule, StorytrackStep storytrack) {
        CapsuleResponse capsuleResponse = new CapsuleResponse(
                updateCapsule.getCapsuleId(),
                updateCapsule.getNickname(),
                updateCapsule.getTitle(),
                updateCapsule.getContent(),
                updateCapsule.getUnlockType(),
                new UnlockResponse(
                        updateCapsule.getUnlockAt(),
                        updateCapsule.getLocationName(),
                        new GPSResponseDTO(
                                updateCapsule.getAddress(),
                                updateCapsule.getLocationLat(),
                                updateCapsule.getLocationLng()
                        ),
                        updateCapsule.getCurrentViewCount()
                )
        );

        return new UpdatePathResponse(
                storytrack.getStorytrack().getStorytrackId(),
                storytrack.getStepOrder(),
                capsuleResponse
        );
    }
}

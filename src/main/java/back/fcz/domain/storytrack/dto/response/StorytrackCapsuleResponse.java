package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.capsule.DTO.response.CapsuleDashBoardResponse;

public record StorytrackCapsuleResponse(
        Long storytrackId,
        int stepOrder,
        CapsuleDashBoardResponse capsule
) {}

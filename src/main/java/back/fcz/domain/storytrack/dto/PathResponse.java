package back.fcz.domain.storytrack.dto;

public record PathResponse(
        int stepOrder,
        CapsuleResponse capsule
) {}
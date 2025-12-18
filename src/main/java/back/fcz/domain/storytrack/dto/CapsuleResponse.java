package back.fcz.domain.storytrack.dto;

public record CapsuleResponse(
        Long capsuleId,
        String capsuleTitle,
        String capsuleContent,
        String unlockType,
        UnlockResponse unlock
) {}

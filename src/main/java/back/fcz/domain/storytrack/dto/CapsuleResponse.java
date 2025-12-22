package back.fcz.domain.storytrack.dto;

import back.fcz.domain.capsule.entity.Capsule;

public record CapsuleResponse(
        Long capsuleId,
        String createrNickname,
        String capsuleTitle,
        String capsuleContent,
        String unlockType,
        UnlockResponse unlock
) {
    public static CapsuleResponse from(Capsule capsule) {
        return new CapsuleResponse(
                capsule.getCapsuleId(),
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getUnlockType(),
                UnlockResponse.from(capsule)
        );
    }
}

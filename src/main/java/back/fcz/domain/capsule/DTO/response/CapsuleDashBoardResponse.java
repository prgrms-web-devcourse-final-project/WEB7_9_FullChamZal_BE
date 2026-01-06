package back.fcz.domain.capsule.DTO.response;

import back.fcz.domain.capsule.entity.Capsule;

import java.time.LocalDateTime;


public record CapsuleDashBoardResponse(
        Long capsuleId,
        String capsuleColor,
        String capsulePackingColor,
        String recipient,
        String sender,
        String title,
        String content,
        LocalDateTime createAt,
        boolean viewStatus,  // 열람 여부
        String unlockType,
        LocalDateTime unlockAt,
        String locationName,
        Double locationLat,
        Double locationLng
) {
    public CapsuleDashBoardResponse (Capsule capsule) {
        this(
                capsule.getCapsuleId(),
                capsule.getCapsuleColor(),
                capsule.getCapsulePackingColor(),
                capsule.getReceiverNickname(),
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getCreatedAt(),
                capsule.getCurrentViewCount() > 0,
                capsule.getUnlockType(),
                capsule.getUnlockAt(),
                capsule.getLocationName(),
                capsule.getLocationLat(),
                capsule.getLocationLng()
        );
    }
}

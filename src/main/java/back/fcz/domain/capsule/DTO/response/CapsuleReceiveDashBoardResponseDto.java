package back.fcz.domain.capsule.DTO.response;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;

import java.time.LocalDateTime;

public record CapsuleReceiveDashBoardResponseDto (
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
){
    public CapsuleReceiveDashBoardResponseDto(Capsule capsule, CapsuleRecipient capsuleRecipient) {
        this(
                capsule.getCapsuleId(),
                capsule.getCapsuleColor(),
                capsule.getCapsulePackingColor(),
                capsuleRecipient.getRecipientName(),
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getCreatedAt(),
                capsule.getCurrentViewCount() > 0,
                capsule.getUnlockType(),
                capsule.getUpdatedAt(),
                capsule.getLocationName(),
                capsule.getLocationLat(),
                capsule.getLocationLng()
        );
    }
}
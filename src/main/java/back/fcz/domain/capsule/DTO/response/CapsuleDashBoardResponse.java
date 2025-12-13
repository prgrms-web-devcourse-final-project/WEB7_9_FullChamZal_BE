package back.fcz.domain.capsule.DTO.response;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;

import java.time.LocalDateTime;

// TODO: CapsuleReadResponseDto와 동일 -> 해당 레코드 삭제 필요 (캡슐 조회 MVP 개발 후, 리팩토링 필요)
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
    public CapsuleDashBoardResponse (Capsule capsule, CapsuleRecipient capsuleRecipient) {
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

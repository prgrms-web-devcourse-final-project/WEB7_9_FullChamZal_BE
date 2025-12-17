package back.fcz.domain.capsule.DTO.request;

import back.fcz.domain.capsule.entity.Capsule;

import java.time.LocalDateTime;

public record SecretCapsuleCreateRequestDTO(
        Long memberId,
        String nickname,
        String receiverNickname,
        String title,
        String content,
        String visibility,
        String unlockType,
        LocalDateTime unlockAt,
        LocalDateTime unlockUntil,
        String locationName,
        String address,
        double locationLat,
        double locationLng,
        int viewingRadius,
        String packingColor,
        String contentColor,
        int maxViewCount
) {
    public Capsule toEntity() {

        return Capsule.builder()
                .nickname(nickname)
                .receiverNickname(receiverNickname)
                .title(title)
                .content(content)
                .capsuleColor(contentColor)
                .capsulePackingColor(packingColor)
                .visibility(visibility)
                .unlockType(unlockType)
                .unlockAt(unlockAt)
                .unlockUntil(unlockUntil)
                .locationName(locationName)
                .address(address)
                .locationLat(locationLat)
                .locationLng(locationLng)
                .locationRadiusM(viewingRadius)
                .maxViewCount(maxViewCount)
                .currentViewCount(0)
                .isDeleted(0)
                .isProtected(0)
                .build();
    }
}

package back.fcz.domain.capsule.DTO.request;

import back.fcz.domain.capsule.entity.Capsule;

import java.time.LocalDateTime;

public record SecretCapsuleCreateRequestDTO(
        Long memberId,
        String nickName,
        String title,
        String content,
        String visibility,
        String unlockType,
        LocalDateTime unlockAt,
        LocalDateTime unlockUntil,
        String locationName,
        double locationLat,
        double locationIng,
        int viewingRadius,
        String packingColor,
        String contentColor,
        int maxViewCount
) {
    public Capsule toEntity() {

        return Capsule.builder()
                .nickname(nickName)
                .title(title)
                .content(content)
                .capsuleColor(contentColor)
                .capsulePackingColor(packingColor)
                .visibility(visibility)
                .unlockType(unlockType)
                .unlockAt(unlockAt)
                .unlockUntil(unlockUntil)
                .locationName(locationName)
                .locationLat(locationLat)
                .locationLng(locationIng)
                .locationRadiusM(viewingRadius)
                .maxViewCount(maxViewCount)
                .currentViewCount(0)
                .isDeleted(0)
                .isProtected(0)
                .build();
    }
}

package back.fcz.domain.capsule.DTO.request;

import back.fcz.domain.capsule.entity.Capsule;

import java.time.LocalDateTime;


public record CapsuleCreateRequestDTO(
        Long memberId,
        String nickname,
        String title,
        String content,
        String capPassword,
        String capsuleColor,
        String capsulePackingColor,
        String visibility,
        String unlockType,
        LocalDateTime unlockAt,
        LocalDateTime unlockUntil,
        String locationName,
        Double locationLat,
        Double locationLng,
        int locationRadiusM,
        int maxViewCount
) {

    public Capsule toEntity() {

        return Capsule.builder()
                .nickname(nickname)
                .title(title)
                .content(content)
                .capPassword(capPassword)
                .capsuleColor(capsuleColor)
                .capsulePackingColor(capsulePackingColor)
                .visibility(visibility)
                .unlockType(unlockType)
                .unlockAt(unlockAt)
                .unlockUntil(unlockUntil)
                .locationName(locationName)
                .locationLat(locationLat)
                .locationLng(locationLng)
                .locationRadiusM(locationRadiusM)
                .maxViewCount(maxViewCount)
                .currentViewCount(0)
                .isDeleted(0)
                .isProtected(0)
                .build();
    }
}


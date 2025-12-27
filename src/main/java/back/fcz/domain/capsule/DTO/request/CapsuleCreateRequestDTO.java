package back.fcz.domain.capsule.DTO.request;

import back.fcz.domain.capsule.entity.Capsule;

import java.time.LocalDateTime;
import java.util.List;


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
        String address,
        Double locationLat,
        Double locationLng,
        int locationRadiusM,
        int maxViewCount,
        List<Long> attachmentIds
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
                .address(address)
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


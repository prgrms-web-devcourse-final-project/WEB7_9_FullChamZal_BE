package back.fcz.domain.capsule.DTO.request;

import back.fcz.domain.capsule.entity.Capsule;

import java.time.LocalDateTime;

public record SecretCapsuleCreateRequestDTO(
        Long memberId,
        String recipientPhone, // 수신자 전화번호 (전화번호 방식일 때 사용)
        String capsulePassword, // 캡슐 비밀번호 (URL + 비밀번호 방식일 때 사용)
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

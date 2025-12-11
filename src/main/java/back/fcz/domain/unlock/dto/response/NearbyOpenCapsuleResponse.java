package back.fcz.domain.unlock.dto.response;

import back.fcz.domain.capsule.entity.Capsule;

import java.time.LocalDateTime;

public record NearbyOpenCapsuleResponse(
        long capsuleId,
        String writerNickname,
        String title,
        String content,
        LocalDateTime capsuleCreatedAt,
        double capsuleLatitude,
        double capsuleLongitude,
        double distanceToCapsule,  // 현재 사용자 위치에서 캡슐까지 남은 거리
        String capsuleUnlockType
) {
    public NearbyOpenCapsuleResponse(Capsule capsule, double distanceToCapsule) {
        this(
                capsule.getCapsuleId(),
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getCreatedAt(),
                capsule.getLocationLat(),
                capsule.getLocationLng(),
                distanceToCapsule,
                capsule.getUnlockType()
        );
    }
}

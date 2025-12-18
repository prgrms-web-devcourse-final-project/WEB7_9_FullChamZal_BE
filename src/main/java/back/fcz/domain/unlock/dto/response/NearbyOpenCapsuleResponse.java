package back.fcz.domain.unlock.dto.response;

import back.fcz.domain.capsule.entity.Capsule;

import java.time.LocalDateTime;

public record NearbyOpenCapsuleResponse(
        long capsuleId,
        String capsuleLocationName,
        String writerNickname,
        String title,
        String content,
        LocalDateTime capsuleCreatedAt,
        String capsuleUnlockType,
        double capsuleLatitude,
        double capsuleLongitude,
        double distanceToCapsule,  // 현재 사용자 위치에서 캡슐까지 남은 거리
        boolean isViewed           // 캡슐 열람 여부 (열람: true, 미열람: false)
) {
    public NearbyOpenCapsuleResponse(Capsule capsule, double distanceToCapsule, boolean isViewed) {
        this(
                capsule.getCapsuleId(),
                capsule.getLocationName(),
                capsule.getNickname(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getCreatedAt(),
                capsule.getUnlockType(),
                capsule.getLocationLat(),
                capsule.getLocationLng(),
                distanceToCapsule,
                isViewed
        );
    }
}

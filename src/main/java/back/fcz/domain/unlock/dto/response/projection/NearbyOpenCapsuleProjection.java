package back.fcz.domain.unlock.dto.response.projection;

import java.time.LocalDateTime;

public record NearbyOpenCapsuleProjection(
        long capsuleId,
        String locationName,
        String nickname,
        String title,
        String content,
        LocalDateTime createdAt,
        String unlockType,
        double locationLat,
        double locationLng,
        int likeCount
) { }

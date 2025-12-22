package back.fcz.domain.storytrack.dto;

import back.fcz.domain.capsule.DTO.GPSResponseDTO;
import back.fcz.domain.capsule.entity.Capsule;

import java.time.LocalDateTime;

public record UnlockResponse(
        LocalDateTime unlockAt,
        String locationName,
        GPSResponseDTO location,
        int currentViewCount
) {
    public static UnlockResponse from(Capsule capsule) {
        return new UnlockResponse(
                capsule.getUnlockAt(),
                capsule.getLocationName(),
                new GPSResponseDTO(
                        capsule.getAddress(),
                        capsule.getLocationLat(),
                        capsule.getLocationLng()
                ),
                capsule.getCurrentViewCount()
        );
    }
}

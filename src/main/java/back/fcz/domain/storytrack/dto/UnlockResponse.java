package back.fcz.domain.storytrack.dto;

import back.fcz.domain.capsule.DTO.GPSResponseDTO;

import java.time.LocalDateTime;

public record UnlockResponse(
        LocalDateTime unlockAt,
        String locationName,
        GPSResponseDTO location,
        int currentViewCount
) {}

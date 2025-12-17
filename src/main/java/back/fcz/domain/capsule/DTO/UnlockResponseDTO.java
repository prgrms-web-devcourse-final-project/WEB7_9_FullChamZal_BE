package back.fcz.domain.capsule.DTO;

import java.time.LocalDateTime;

public record UnlockResponseDTO(
        LocalDateTime unlockAt,
        LocalDateTime unlockUntil,
        String location,
        GPSResponseDTO gps,
        int viewingRadius
){}

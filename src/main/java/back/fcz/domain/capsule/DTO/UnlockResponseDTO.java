package back.fcz.domain.capsule.DTO;

import java.time.LocalDateTime;

public record UnlockResponseDTO(
        LocalDateTime unlockAt,
        String location,
        GPSResponseDTO gps,
        int viewingRadius
){}

package back.fcz.domain.capsule.DTO.request;

import java.time.LocalDateTime;

public record CapsuleReadRequestDto (
        Long capsuleId,
        String phoneNumber,
        LocalDateTime unlockAt,
        Double locationLat,
        Double locationLng,

        String url,
        String password
){
}

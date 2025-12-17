package back.fcz.domain.capsule.DTO;

public record GPSResponseDTO(
        String address,
        Double locationLat,
        Double locationLng
) { }

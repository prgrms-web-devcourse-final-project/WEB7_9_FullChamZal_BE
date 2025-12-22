package back.fcz.domain.unlock.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record NearbyOpenCapsuleRequest(
        @NotNull(message = "시간 값은 필수입니다")
        LocalDateTime currentTime,

        @NotNull(message = "위도 값은 필수입니다")
        @Min(value = -90, message = "위도 값은 -90 이상이어야 합니다")
        @Max(value = 90, message = "위도 값은 90 이하여야 합니다")
        Double currentLatitude,

        @NotNull(message = "경도 값은 필수입니다")
        @Min(value = -180, message = "경도 값은 -180 이상이어야 합니다")
        @Max(value = 180, message = "경도 값은 180 이하여야 합니다")
        Double currentLongitude,

        Integer radius
) {}

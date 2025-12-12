package back.fcz.domain.capsule.dto.response;

import java.util.List;

public record CapsuleSendDashBoardResponseDto(
        List<CapsuleReadResponseDto>  capsuleList
) {
}

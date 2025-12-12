package back.fcz.domain.capsule.DTO.response;

import java.util.List;

public record CapsuleSendDashBoardResponseDTO(
        List<CapsuleReadResponseDTO> capsuleList
) {
}

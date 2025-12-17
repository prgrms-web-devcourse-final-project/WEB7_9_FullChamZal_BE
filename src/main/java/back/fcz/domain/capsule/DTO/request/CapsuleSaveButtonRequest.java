package back.fcz.domain.capsule.DTO.request;

import java.time.LocalDateTime;

public record CapsuleSaveButtonRequest(
        Long capsuleId,
        Integer isSendSelf,  // 타인에게 보내는 경우 0, 본인에게 보내는 경우 1
        LocalDateTime unlockAt
) {
}

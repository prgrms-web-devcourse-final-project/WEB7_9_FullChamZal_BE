package back.fcz.domain.capsule.DTO.response;


import back.fcz.domain.capsule.entity.CapsuleRecipient;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UnlockedCapsuleResponse {
    private Long capsuleId;
    private String sender;           // 보낸 사람
    private LocalDateTime unlockAt;  // 해제 시간
    private String locationName;     // 해제 위치

    public UnlockedCapsuleResponse(CapsuleRecipient recipient) {
        this.capsuleId = recipient.getCapsuleId().getCapsuleId();
        this.sender = recipient.getCapsuleId().getNickname();
        this.unlockAt = recipient.getCapsuleId().getUnlockAt();
        this.locationName = recipient.getCapsuleId().getLocationName();
    }
}

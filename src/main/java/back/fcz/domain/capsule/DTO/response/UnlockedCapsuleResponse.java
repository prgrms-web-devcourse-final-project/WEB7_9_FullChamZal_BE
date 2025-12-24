package back.fcz.domain.capsule.DTO.response;


import back.fcz.domain.capsule.entity.CapsuleRecipient;
import lombok.Getter;

@Getter
public class UnlockedCapsuleResponse {
    private Long capsuleId;
    private String sender;    // 보낸 사람
    private String receiver;   // 받는 사람
    private String unlockType;      // 해제 조건

    public UnlockedCapsuleResponse(CapsuleRecipient recipient) {
        this.capsuleId = recipient.getCapsuleId().getCapsuleId();
        this.sender = recipient.getCapsuleId().getNickname();
        this.receiver = recipient.getCapsuleId().getReceiverNickname();
        this.unlockType = recipient.getCapsuleId().getUnlockType();
    }
}

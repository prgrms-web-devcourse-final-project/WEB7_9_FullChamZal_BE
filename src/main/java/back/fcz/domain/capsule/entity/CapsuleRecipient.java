package back.fcz.domain.capsule.entity;


import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "capsule_recipient")
public class CapsuleRecipient extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "capsule_id",nullable = false)
    private Capsule capsuleId;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    @Column(name = "recipient_phone_hash", nullable = false)
    private String recipientPhoneHash;

    @Column(name = "is_sender_self", nullable = false)
    private Integer isSenderSelf;   // 타인에게 보내는 경우 0, 본인에게 보내는 경우 1

    @Setter
    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

}


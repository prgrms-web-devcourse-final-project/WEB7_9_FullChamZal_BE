package back.fcz.domain.capsule.entity;


import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "capsule_recipient")
class CapsuleRecipient extends BaseEntity {
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
    private boolean isSenderSelf;
}


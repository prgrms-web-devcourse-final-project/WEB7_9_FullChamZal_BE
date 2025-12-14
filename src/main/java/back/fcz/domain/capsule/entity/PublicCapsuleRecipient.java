package back.fcz.domain.capsule.entity;

import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PublicCapsuleRecipient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsuleId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "unlocked_at", nullable = false)
    private LocalDateTime unlockedAt;
}
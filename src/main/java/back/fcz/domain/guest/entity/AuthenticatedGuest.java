package back.fcz.domain.guest.entity;

import back.fcz.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "authenticated_guest",
        indexes = {
            @Index(name = "uk_guest_phone", columnList = "phone_number_hash", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AuthenticatedGuest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number_hash", nullable = false, length = 64, unique = true)
    private String phoneNumberHash;

    @Column(name = "phone_number", nullable = false, length = 500)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 64)
    private String passwordHash;

    @Column(name = "consent_marketing", nullable = false)
    private boolean consentMarketing = false;

    @Column(name = "consent_at", nullable = false)
    private LocalDateTime consentAt;

    @Column(name = "last_capsule_received_at", nullable = false)
    private LocalDateTime lastCapsuleReceivedAt;

    @Column(name = "expiry_at", nullable = false)
    private LocalDateTime expiryAt;

    @PrePersist
    public void giveMarketingConsent() {
        this.consentAt = LocalDateTime.now();
    }
    public void updateLastCapsuleReceivedAt(LocalDateTime now) {
        this.lastCapsuleReceivedAt = now;
        this.expiryAt = now.plusDays(30);
    }

}

package back.fcz.domain.sms.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "phone_verifications",
        indexes = {
            @Index(name = "idx_phone_purpose_created", columnList = "phone_number, purpose, created_at"),
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PhoneVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 500)
    private String phoneNumber;

    @Column(name = "phone_number_hash", nullable = false, length = 64)
    private String phoneNumberHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private PhoneVerificationPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PhoneVerificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.expiredAt = LocalDateTime.now().plusMinutes(3);  // 인증 번호 요청 후 3분 후 만료
        this.status = PhoneVerificationStatus.PENDING;
    }

    public boolean isExpired(LocalDateTime now){    // 현재 시간이 만료 시간 이후인지 확인
        return now.isAfter(expiredAt);
    }
    public void markVerified(LocalDateTime now){   // 인증 성공 시 상태 변경 및 인증 시간 기록
        this.status = PhoneVerificationStatus.VERIFIED;
        this.verifiedAt = now;
    }
    public void incrementAttemptCount(){    // 인증 시도 횟수 증가 : API호출 제약
        this.attemptCount++;
    }
}

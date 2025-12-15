package back.fcz.domain.sms.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "phone_verifications",
        indexes = {
            @Index(name = "idx_phone_purpose_created", columnList = "phone_number_hash, purpose, created_at"),
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification {
    private static final int CODE_EXPIRATION_MINUTES = 3; // 인증 번호 만료 시간 (분)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number_hash", nullable = false, length = 64)
    private String phoneNumberHash;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private PhoneVerificationPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PhoneVerificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public PhoneVerification(String phoneNumberHash,
                             String code,
                             PhoneVerificationPurpose purpose) {
        this.phoneNumberHash = phoneNumberHash;
        this.code = code;
        this.purpose = purpose;
    }

    @PrePersist
    protected void onCreate(){
        if(this.createdAt == null){     // baseinit이나 test init이 아닌 경우에만 설정
            this.createdAt = LocalDateTime.now();
            this.expiredAt = LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);  // 인증 번호 요청 후 3분 후 만료
            this.status = PhoneVerificationStatus.PENDING;
            this.attemptCount = 0;
        }
    }

    public boolean isExpired(LocalDateTime now){    // 현재 시간이 만료 시간 이후인지 확인
        return now.isAfter(expiredAt);
    }
    public void markExpired(){    // 만료 상태로 변경
        this.status = PhoneVerificationStatus.EXPIRED;
    }
    public void markVerified(LocalDateTime now){   // 인증 성공 시 상태 변경 및 인증 시간 기록
        this.status = PhoneVerificationStatus.VERIFIED;
        this.verifiedAt = now;
    }
    public void incrementAttemptCount(){    // 인증 시도 횟수 증가 : API호출 제약
        this.attemptCount++;
    }

    public void reset(String newCode, LocalDateTime now){
        this.code = newCode;
        this.expiredAt = now.plusMinutes(CODE_EXPIRATION_MINUTES);
        this.status = PhoneVerificationStatus.PENDING;
        this.attemptCount = 0;
    }





    /* init용 */
    public static PhoneVerification initForTest(
            String phoneNumberHash,
            String code,
            PhoneVerificationPurpose purpose,
            PhoneVerificationStatus status,
            int attemptCount,
            LocalDateTime createdAt,
            LocalDateTime verifiedAt,
            LocalDateTime expiredAt
    ) {
        PhoneVerification pv = new PhoneVerification(
                phoneNumberHash,
                code,
                purpose
        );
        pv.status = status;
        pv.attemptCount = attemptCount;
        pv.createdAt = createdAt;
        pv.verifiedAt = verifiedAt;
        pv.expiredAt = expiredAt;
        return pv;
    }

}

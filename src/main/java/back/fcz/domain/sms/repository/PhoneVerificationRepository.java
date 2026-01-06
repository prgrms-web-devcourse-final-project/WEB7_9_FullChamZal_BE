package back.fcz.domain.sms.repository;

import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.entity.PhoneVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification,Long> {

    long countByPhoneNumberHashAndPurposeAndCreatedAtAfter(
            String phoneNumberHash,
            PhoneVerificationPurpose purpose,
            LocalDateTime cooldownThreshold
    );
    Optional<PhoneVerification> findTop1ByPhoneNumberHashAndPurposeOrderByCreatedAtDesc(
            String phoneNumberHash,
            PhoneVerificationPurpose purpose
    );

    Optional<PhoneVerification> findTop1ByPhoneNumberHashAndPurposeAndStatusOrderByCreatedAtDesc(
            String phoneNumberHash,
            PhoneVerificationPurpose purpose,
            PhoneVerificationStatus status
    );

    @Modifying
    @Query("delete from PhoneVerification pv where pv.createdAt < :expiredAt")
    int deleteExpired(@Param("expiredAt") LocalDateTime expiredAt);

    // 기존 코드 호환용
    default Optional<PhoneVerification> findLatestPending(String phoneNumberHash, PhoneVerificationPurpose purpose) {
        return findTop1ByPhoneNumberHashAndPurposeAndStatusOrderByCreatedAtDesc(
                phoneNumberHash,
                purpose,
                PhoneVerificationStatus.PENDING
        );
    }

    List<PhoneVerification> findTop5ByPhoneNumberHashOrderByCreatedAtDesc(String phoneNumberHash);

    Page<PhoneVerification> findByPurpose(PhoneVerificationPurpose purpose, Pageable pageable);
    Page<PhoneVerification> findByStatus(PhoneVerificationStatus status, Pageable pageable);

    Page<PhoneVerification> findByPurposeAndStatus(
            PhoneVerificationPurpose purpose,
            PhoneVerificationStatus status,
            Pageable pageable
    );

    boolean existsByPhoneNumberHashAndStatus(String phoneNumberHash, PhoneVerificationStatus phoneVerificationStatus);
}

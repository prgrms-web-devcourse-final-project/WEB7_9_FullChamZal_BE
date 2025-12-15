package back.fcz.domain.sms.repository;

import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.entity.PhoneVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification,Long> {
    long countByPhoneNumberHashAndPurposeAndCreatedAtAfter(
            String PhoneNumberHash,
            PhoneVerificationPurpose purpose,
            LocalDateTime cooldownThreshold);

    @Query("""
            select pv
            from PhoneVerification pv
            where pv.phoneNumberHash = :phoneNumberHash
              and pv.purpose = :purpose
              and pv.status = 'PENDING'
            order by pv.createdAt desc
            limit 1
            """)
    Optional<PhoneVerification> findLatestPending(
            String phoneNumberHash,
            PhoneVerificationPurpose purpose);

    Optional<PhoneVerification> findTop1ByPhoneNumberHashAndPurposeOrderByCreatedAtDesc(
            String phoneNumberHash,
            PhoneVerificationPurpose purpose);

    Page<PhoneVerification> findByPurpose(PhoneVerificationPurpose purpose, Pageable pageable);

    Page<PhoneVerification> findByStatus(PhoneVerificationStatus status, Pageable pageable);

    Page<PhoneVerification> findByPurposeAndStatus(
            PhoneVerificationPurpose purpose,
            PhoneVerificationStatus status,
            Pageable pageable
    );
}

package back.fcz.domain.sms.repository;

import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

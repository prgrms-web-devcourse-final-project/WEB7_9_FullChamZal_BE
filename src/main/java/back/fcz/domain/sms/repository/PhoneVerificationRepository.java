package back.fcz.domain.sms.repository;

import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.entity.PhoneVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification,Long> {
    long countByPhoneNumberHashAndPurposeAndCreatedAtAfter(
            String PhoneNumberHash,
            PhoneVerificationPurpose purpose,
            LocalDateTime cooldownThreshold);



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


    long countByPhoneNumberHash(String phoneNumberHash);


    Optional<PhoneVerification> findTop1ByPhoneNumberHashAndPurposeAndStatusOrderByCreatedAtDesc(
            String phoneNumberHash,
            PhoneVerificationPurpose purpose,
            PhoneVerificationStatus status
    );

    // 기존에 "latest pending"이 필요하면 이렇게 써야 함 (JPQL limit 금지)
    default Optional<PhoneVerification> findLatestPending(String phoneNumberHash, PhoneVerificationPurpose purpose) {
        return findTop1ByPhoneNumberHashAndPurposeAndStatusOrderByCreatedAtDesc(
                phoneNumberHash,
                purpose,
                PhoneVerificationStatus.PENDING
        );
    }

    List<PhoneVerification> findTop5ByPhoneNumberHashOrderByCreatedAtDesc(String phoneNumberHash);

}

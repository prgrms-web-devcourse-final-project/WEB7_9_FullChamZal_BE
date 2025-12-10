package back.fcz.domain.sms.repository;

import back.fcz.domain.sms.entity.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification,Long> {
}

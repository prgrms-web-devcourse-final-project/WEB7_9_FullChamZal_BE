package back.fcz.domain.sms.service;

import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.repository.PhoneVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhoneVerificationAttemptService {
    private final PhoneVerificationRepository phoneVerificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(Long id){
        PhoneVerification pv = phoneVerificationRepository.findById(id)
                .orElseThrow();
        pv.incrementAttemptCount();
    }
}

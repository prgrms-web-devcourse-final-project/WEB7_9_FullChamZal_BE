package back.fcz.domain.sms.scheduler;

import back.fcz.domain.sms.repository.PhoneVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class PhoneverificationDeleteSchedulerService {
    private final PhoneVerificationRepository phoneVerificationRepository;

    /**
     * 매일 새벽 3시에 30일 지난 인증 내역 삭제
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredPhoneVerification() {
        LocalDateTime expiredAt = LocalDateTime.now().minusDays(30);

        int deletedCount = phoneVerificationRepository.deleteExpired(expiredAt);

        log.info("[PhoneVerificationScheduler] deleted count = {}", deletedCount);
    }
}

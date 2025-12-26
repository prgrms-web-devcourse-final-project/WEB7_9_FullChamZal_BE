package back.fcz.domain.capsule.scheduler;

import back.fcz.domain.capsule.service.CapsuleHardDeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test") // 테스트에서 스케줄 자동 실행 방지
@RequiredArgsConstructor
public class CapsuleHardDeleteScheduler {

    private final CapsuleHardDeleteService capsuleHardDeleteService;

    // 기본: 매일 새벽 4시 (원하면 yml에서 capsule.hard-delete.cron 으로 덮어쓰기)
    @Scheduled(cron = "${capsule.hard-delete.cron:0 0 4 * * *}")
    public void run() {
        int deleted = capsuleHardDeleteService.hardDeleteOnce(100);
        if (deleted > 0) {
            log.info("[CapsuleHardDeleteScheduler] deleted={}", deleted);
        }
    }
}

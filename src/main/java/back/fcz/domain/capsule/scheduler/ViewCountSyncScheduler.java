package back.fcz.domain.capsule.scheduler;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewCountSyncScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final CapsuleRepository capsuleRepository;

    private static final String VIEW_COUNT_KEY_PREFIX = "capsule:view:";

    @Scheduled(fixedRate = 900000)
    @Transactional
    public void syncViewCountsToDB() {
        Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        int syncCount = 0;
        int errorCount = 0;

        for (String key : keys) {
            try {
                Long capsuleId = Long.parseLong(key.replace(VIEW_COUNT_KEY_PREFIX, ""));
                String countStr = redisTemplate.opsForValue().get(key);

                if (countStr == null || countStr.equals("0")) {
                    redisTemplate.delete(key);
                    continue;
                }

                // 조회수가 0보다 크면 동기화 진행
                int redisCount = Integer.parseInt(countStr);

                Capsule capsule = capsuleRepository.findById(capsuleId)
                        .orElse(null);

                if (capsule == null) {
                    // 캡슐이 삭제되었으면 Redis 키만 삭제
                    redisTemplate.delete(key);
                    continue;
                }

                // 선착순 캡슐은 동기화 제외 (Redis 키만 삭제)
                if (hasFirstComeLimit(capsule)) {
                    log.debug("선착순 캡슐 동기화 건너뜀 - capsuleId: {}", capsuleId);
                    redisTemplate.delete(key);
                    continue;
                }

                capsule.increasedViewCount(redisCount);
                syncCount++;

                redisTemplate.delete(key);
            } catch (Exception e) {
                log.error("조회수 동기화 실패 - key: {}", key, e);
                errorCount++;
            }
        }

        if (syncCount > 0 || errorCount > 0) {
            log.info("조회수 배치 동기화 완료 - 성공: {}, 실패: {}", syncCount, errorCount);
        }
    }

    private boolean hasFirstComeLimit(Capsule capsule) {
        Integer maxViewCount = capsule.getMaxViewCount();
        return maxViewCount != null && maxViewCount > 0;
    }
}

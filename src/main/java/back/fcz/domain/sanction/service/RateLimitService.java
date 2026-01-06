package back.fcz.domain.sanction.service;

import back.fcz.domain.sanction.constant.RiskLevel;
import back.fcz.domain.sanction.properties.SanctionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

// Redis 기반 Rate Limiting 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SanctionProperties sanctionProperties;

    // Redis 키 접두사
    private static final String RATE_LIMIT_KEY_MEMBER = "rate_limit:member:";
    private static final String RATE_LIMIT_KEY_IP = "rate_limit:ip:";

    /* ==========================
       외부 진입 포인트
       ========================== */

    public void apply(Long memberId, RiskLevel riskLevel) {
        String key = RATE_LIMIT_KEY_MEMBER + memberId;
        applyInternal(key, riskLevel, "회원 " + memberId);
    }

    public void applyByIp(String ipAddress, RiskLevel riskLevel) {
        String key = RATE_LIMIT_KEY_IP + ipAddress;
        applyInternal(key, riskLevel, "IP " + ipAddress);
    }

    public void applyCooldown(Long memberId, int minutes) {
        String key = RATE_LIMIT_KEY_MEMBER + memberId;
        redisTemplate.expire(key, Duration.ofMinutes(minutes));
        log.warn("RateLimit 쿨다운 적용: 회원 {}, {}분", memberId, minutes);
    }

    public void applyCooldownByIp(String ipAddress, int minutes) {
        String key = RATE_LIMIT_KEY_IP + ipAddress;
        redisTemplate.expire(key, Duration.ofMinutes(minutes));
        log.warn("RateLimit 쿨다운 적용: IP {}, {}분", ipAddress, minutes);
    }

    public boolean isInCooldown(Long memberId) {
        String key = RATE_LIMIT_KEY_MEMBER + memberId;
        return isInCooldownInternal(key);
    }

    public boolean isInCooldownByIp(String ipAddress) {
        String key = RATE_LIMIT_KEY_IP + ipAddress;
        return isInCooldownInternal(key);
    }

    /* ==========================
       핵심 로직
       ========================== */

    private void applyInternal(String key, RiskLevel riskLevel, String identifier) {

        var rateLimit = sanctionProperties.getRateLimit();

        int windowSeconds = rateLimit.getWindowSeconds().get(riskLevel);
        int maxRequests = rateLimit.getMaxRequests().get(riskLevel);
        int cooldownSeconds = rateLimit.getCooldownSeconds().get(riskLevel);

        long now = System.currentTimeMillis();

        try {
            // 1. 현재 TTL 확인
            Long currentTtl = redisTemplate.getExpire(key);

            // 2. 이미 쿨다운 중이면 요청 추가하지 않음
            if (currentTtl != null && currentTtl > windowSeconds) {
                log.warn("쿨다운 중 요청 차단: {}, 남은 시간: {}초", identifier, currentTtl);
                return;
            }

            // 3. 윈도우 밖 요청 제거
            redisTemplate.opsForZSet()
                    .removeRangeByScore(key, 0, now - windowSeconds * 1000L);

            // 4. 현재 요청 기록
            redisTemplate.opsForZSet()
                    .add(key, String.valueOf(now), now);

            // 5. 요청 수 계산
            Long requestCount = redisTemplate.opsForZSet().zCard(key);

            // 6. TTL 설정 (윈도우 기준)
            if (requestCount == null || requestCount <= maxRequests) {
                // 정상: 윈도우 TTL
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            } else {
                // 초과: 쿨다운 TTL
                log.warn("RateLimit 초과: {} ({}회 / {}초), 쿨다운 {}초 적용",
                        identifier, requestCount, windowSeconds, cooldownSeconds);
                redisTemplate.expire(key, Duration.ofSeconds(cooldownSeconds));
            }
        } catch (Exception e) {
            log.error("RateLimit 처리 실패: {}", identifier, e);
            // 장애 시 서비스는 계속 동작
        }
    }

    private boolean isInCooldownInternal(String key) {
        try {
            Long count = redisTemplate.opsForZSet().zCard(key);
            if (count == null || count == 0) {
                return false;
            }

            Long ttl = redisTemplate.getExpire(key);
            if (ttl == null || ttl <= 0) {
                return false;
            }

            // 윈도우 시간보다 TTL이 크면 쿨다운 상태
            var rateLimit = sanctionProperties.getRateLimit();
            int windowSeconds = rateLimit.getWindowSeconds().get(RiskLevel.LOW);

            return ttl > windowSeconds;
        } catch (Exception e) {
            log.error("쿨다운 확인 실패: {}", key, e);
            return false;
        }
    }
}
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
            // 윈도우 밖 요청 제거
            redisTemplate.opsForZSet()
                    .removeRangeByScore(key, 0, now - windowSeconds * 1000L);

            // 현재 요청 기록
            redisTemplate.opsForZSet()
                    .add(key, String.valueOf(now), now);

            // 요청 수 계산
            Long requestCount = redisTemplate.opsForZSet().zCard(key);

            // TTL 설정 (윈도우 기준)
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));

            if (requestCount != null && requestCount > maxRequests) {
                log.warn("RateLimit 초과: {} ({}회 / {}초)",
                        identifier, requestCount, windowSeconds);

                // 쿨다운 적용
                redisTemplate.expire(key, Duration.ofSeconds(cooldownSeconds));
            }

        } catch (Exception e) {
            log.error("RateLimit 처리 실패: {}", identifier, e);
            // 장애 시 서비스는 계속 동작
        }
    }
}
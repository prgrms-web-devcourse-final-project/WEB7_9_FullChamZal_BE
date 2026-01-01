package back.fcz.domain.sanction.service;

import back.fcz.domain.sanction.constant.RiskLevel;
import back.fcz.domain.sanction.properties.SanctionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

// Redis 기반 의심 점수 모니터링 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SanctionService sanctionService;
    private final IpBlockService ipBlockService;
    private final RateLimitService rateLimitService;
    private final SanctionProperties sanctionProperties;

    // Redis 키 접두사
    private static final String SUSPICION_KEY_PREFIX_MEMBER = "suspicion:member:";
    private static final String SUSPICION_KEY_PREFIX_IP = "suspicion:ip:";

    // 회원의 의심 점수 증가
    public void incrementSuspicionScore(Long memberId, int score) {
        String key = SUSPICION_KEY_PREFIX_MEMBER + memberId;
        int currentScore = incrementScore(key, score, "회원 " + memberId);

        // 임계값 확인 및 제재 적용
        checkAndApplySanction(memberId, null, currentScore);
    }

    //  비회원(IP)의 의심 점수 증가
    public void incrementSuspicionScoreByIp(String ipAddress, int score) {
        String key = SUSPICION_KEY_PREFIX_IP + ipAddress;
        int currentScore = incrementScore(key, score, "IP " + ipAddress);

        var thresholds = sanctionProperties.getMonitoring().getThresholds();

        // 임계값 확인
        if (currentScore >= thresholds.getLimit()) {
            log.error("IP 차단 필요: {} (점수: {}점)", ipAddress, currentScore);
            ipBlockService.blockIp(
                    ipAddress,
                    "의심 활동 누적 (점수: " + currentScore + "점)"
            );
            resetSuspicionScoreByIp(ipAddress);
        } else if (currentScore >= thresholds.getWarning()) {
            log.warn("IP 제한 필요: {} (점수: {}점)", ipAddress, currentScore);
            int cooldownMinutes =
                    sanctionProperties.getRateLimit()
                            .getCooldownSeconds()
                            .get(RiskLevel.MEDIUM) / 60;

            rateLimitService.applyCooldownByIp(ipAddress, cooldownMinutes);
        }
    }

    // 임계값 확인하고 필요 시 제재
    private void checkAndApplySanction(Long memberId, String ipAddress, int currentScore) {
        var thresholds = sanctionProperties.getMonitoring().getThresholds();

        if (currentScore >= thresholds.getLimit()) {
            // 차단 수준: 계정 정지
            log.error("자동 제재 발동: 회원 {} (점수: {}점) - 계정 정지 7일", memberId, currentScore);
            sanctionService.applyAutoSuspension(
                    memberId,
                    "의심 활동 누적 (점수: " + currentScore + "점)",
                    7
            );
            // 제재 후 점수 초기화
            resetSuspicionScore(memberId);
        } else if (currentScore >= thresholds.getWarning()) {
            // 제한 수준: Rate limiting 적용
            log.warn("Rate Limiting 적용 대상: 회원 {} (점수: {}점)", memberId, currentScore);
            int cooldownMinutes =
                    sanctionProperties.getRateLimit()
                            .getCooldownSeconds()
                            .get(RiskLevel.MEDIUM) / 60;
            rateLimitService.applyCooldown(memberId, cooldownMinutes); // 30분 쿨다운
        }
    }

    // 공통 점수 증가 로직
    private int incrementScore(String key, int score, String identifier) {
        try {
            Long newScore = redisTemplate.opsForValue().increment(key, score);

            Long ttlSeconds = redisTemplate.getExpire(key);
            if (ttlSeconds == null || ttlSeconds == -1L || ttlSeconds == -2L) {
                Duration ttl = sanctionProperties.getMonitoring().getSuspicionTtl();
                redisTemplate.expire(key, ttl);
            }

            return newScore != null ? newScore.intValue() : 0;
        } catch (Exception e) {
            log.error("Redis 점수 증가 실패: {}", identifier, e);
            return 0;
        }
    }

    // 공통 점수 조회 로직
    private int getScore(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Integer.parseInt(value) : 0;
        } catch (Exception e) {
            log.error("Redis 점수 조회 실패: {}", key, e);
            return 0; // 조회 실패 시 0 반환
        }
    }

    // 회원의 현재 의심 점수 조회
    public int getSuspicionScore(Long memberId) {
        String key = SUSPICION_KEY_PREFIX_MEMBER + memberId;
        return getScore(key);
    }

    // 비회원(IP)의 현재 의심 점수 조회
    public int getSuspicionScoreByIp(String ipAddress) {
        String key = SUSPICION_KEY_PREFIX_IP + ipAddress;
        return getScore(key);
    }

    // 회원의 의심 점수 초기화
    public void resetSuspicionScore(Long memberId) {
        String key = SUSPICION_KEY_PREFIX_MEMBER + memberId;
        try {
            redisTemplate.delete(key);
            log.info("의심 점수 초기화 완료: 회원 {}", memberId);
        } catch (Exception e) {
            log.error("Redis 점수 초기화 실패: 회원 {}", memberId, e);
        }
    }

    // 비회원(IP)의 의심 점수 초기화
    public void resetSuspicionScoreByIp(String ipAddress) {
        String key = SUSPICION_KEY_PREFIX_IP + ipAddress;
        try {
            redisTemplate.delete(key);
            log.info("의심 점수 초기화 완료: IP {}", ipAddress);
        } catch (Exception e) {
            log.error("Redis 점수 초기화 실패: IP {}", ipAddress, e);
        }
    }
}

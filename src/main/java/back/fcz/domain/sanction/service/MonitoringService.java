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
        checkAndApplySanctionForMember(memberId, currentScore);
    }

    //  비회원(IP)의 의심 점수 증가
    public void incrementSuspicionScoreByIp(String ipAddress, int score) {
        String key = SUSPICION_KEY_PREFIX_IP + ipAddress;
        int currentScore = incrementScore(key, score, "IP " + ipAddress);

        checkAndApplySanctionForIp(ipAddress, currentScore);
    }

    // 임계값 확인하고 필요 시 제재 - 멤버
    private void checkAndApplySanctionForMember(Long memberId, int currentScore) {
        var thresholds = sanctionProperties.getMonitoring().getThresholds();

        if (currentScore >= thresholds.getLimit()) {
            sanctionService.applyAutoSuspension(
                    memberId,
                    "의심 활동 누적 (점수: " + currentScore + "점)",
                    7
            );
            resetSuspicionScore(memberId);
        } else if (currentScore >= thresholds.getWarning()) {
            int cooldownSeconds = sanctionProperties.getRateLimit()
                    .getCooldownSeconds().get(RiskLevel.MEDIUM);
            rateLimitService.applyCooldown(memberId, cooldownSeconds / 60);
        }
    }

    // 임계값 확인하고 필요 시 제재 - 비회원
    private void checkAndApplySanctionForIp(String ipAddress, int currentScore) {
        var thresholds = sanctionProperties.getMonitoring().getThresholds();

        if (currentScore >= thresholds.getLimit()) {
            ipBlockService.blockIp(
                    ipAddress,
                    "의심 활동 누적 (점수: " + currentScore + "점)"
            );
            resetSuspicionScoreByIp(ipAddress);
        } else if (currentScore >= thresholds.getWarning()) {
            int cooldownSeconds = sanctionProperties.getRateLimit()
                    .getCooldownSeconds().get(RiskLevel.MEDIUM);
            rateLimitService.applyCooldownByIp(ipAddress, cooldownSeconds / 60);
        }
    }

    // 공통 점수 증가 로직
    private int incrementScore(String key, int score, String identifier) {
        try {
            Long newScore = redisTemplate.opsForValue().increment(key, score);

            Duration ttl = sanctionProperties.getMonitoring().getSuspicionTtl();
            redisTemplate.expire(key, ttl);

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

package back.fcz.domain.sanction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

// Redis 기반 IP 차단 관리 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class IpBlockService {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis 키 접두사
    private static final String BLOCKED_IP_PREFIX = "blocked:ip:";

    // 기본 차단 기간 (7일)
    private static final Duration DEFAULT_BLOCK_DURATION = Duration.ofDays(7);

    // IP를 차단 목록에 추가
    public void blockIp(String ipAddress, String reason, int durationDays) {
        String key = BLOCKED_IP_PREFIX + ipAddress;
        String value = reason + "|" + System.currentTimeMillis();

        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofDays(durationDays));
            log.warn("IP 차단 완료: {} (사유: {}, 기간: {}일)", ipAddress, reason, durationDays);
        } catch (Exception e) {
            log.error("IP 차단 실패: {}", ipAddress, e);
        }
    }

    // IP를 7일 동안 차단
    public void blockIp(String ipAddress, String reason) {
        blockIp(ipAddress, reason, 7);
    }

    // IP 차단 유무 확인
    public boolean isBlocked(String ipAddress) {
        String key = BLOCKED_IP_PREFIX + ipAddress;

        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null;
        } catch (Exception e) {
            log.error("IP 차단 상태 조회 실패: {}", ipAddress, e);
            // Redis 장애 시 false 반환 (정상 접근 허용)
            return false;
        }
    }

    // IP 차단 해제
    public void unblockIp(String ipAddress) {
        String key = BLOCKED_IP_PREFIX + ipAddress;

        try {
            redisTemplate.delete(key);
            log.info("IP 차단 해제 완료: {}", ipAddress);
        } catch (Exception e) {
            log.error("IP 차단 해제 실패: {}", ipAddress, e);
        }
    }

    // 차단 사유 조회
    public String getBlockReason(String ipAddress) {
        String key = BLOCKED_IP_PREFIX + ipAddress;

        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && value.contains("|")) {
                return value.split("\\|")[0];
            }
            return value;
        } catch (Exception e) {
            log.error("IP 차단 사유 조회 실패: {}", ipAddress, e);
            return null;
        }
    }
}

package back.fcz.domain.backup.service;

import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 구글 Access Token 관리 서비스
 *  - 구글 Access Token을 Redis에 저장, 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String GOOGLE_ACCESS_TOKEN_KEY_PREFIX = "google:access:";

    // access 토큰을 Redis에 저장
    public void saveAccessToken(Long memberId, String accessToken, long expiresIn) {
        try {
            // 구글이 준 만료 시간보다 5분 일찍 만료시켜, 안전하게 갱신되도록 ttl 설정
            long ttl = Math.max(expiresIn - 300, 60);

            redisTemplate.opsForValue().set(
                    GOOGLE_ACCESS_TOKEN_KEY_PREFIX + memberId,
                    accessToken,
                    ttl,
                    TimeUnit.SECONDS
            );
            log.info("Google Access Token 저장 완료. memberId: {}, TTL: {}초", memberId, ttl);

        } catch (Exception e) {
            log.error("Google Access Token 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_ERROR);
        }
    }

    // access 토큰 조회
    public Optional<String> getAccessToken(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(GOOGLE_ACCESS_TOKEN_KEY_PREFIX + memberId));
    }
}

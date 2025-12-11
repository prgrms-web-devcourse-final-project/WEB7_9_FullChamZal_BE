package back.fcz.global.security.jwt.service;

import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

// 로그아웃 토큰을 Redis에 저장하여 재사용 방지
@Slf4j
@Service@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProvider jwtProvider;

    private static final String BLACKLIST_KEY_PREFIX = "blacklist";

    /**
     * 로그아웃 시 호출, 토큰의 남은 유효 시간만큼 Redis에 저장해 두어서,
     * 동일한 AccessToken 사용을 방지
     * */
    public void addToBlacklist(String accessToken) {
        try {
            long ttl = jwtProvider.getExpirationSeconds(accessToken);

            if(ttl < 0) {
                log.debug("이미 만료된 토큰이므로 블랙리스트에 추가하지 않음.");
                return;
            }

            String key = BLACKLIST_KEY_PREFIX + ":" + accessToken;
            redisTemplate.opsForValue().set(
                    key,
                    "logged_out",
                    ttl,
                    TimeUnit.SECONDS
            );

            log.info("토큰 블랙리스트 추가 완료. TTL: {}초", ttl);
        } catch (BusinessException e) {
            log.error("토큰 파싱 중 오류 발생: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("블랙리스트 추가 중 Redis 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    // 토큰이 블랙리스트에 있는지 확인
    public boolean isBlacklisted(String accessToken) {
        try {
            String key = BLACKLIST_KEY_PREFIX + ":" + accessToken;
            Boolean exists = redisTemplate.hasKey(key);

            boolean result = Boolean.TRUE.equals(exists);

            if (result) {
                log.warn("블랙리스트에 등록된 토큰 사용 시도 감지");
            }

            return result;
        } catch (Exception e) {
            log.error("블랙리스트 확인 중 Redis 연결 오류 발생. 토큰: {}, 오류: {}",
                    accessToken.substring(0, Math.min(20, accessToken.length())) + "...",
                    e.getMessage(), e);

            throw new BusinessException(ErrorCode.REDIS_CONNECTION_ERROR);
        }
    }
}

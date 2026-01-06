package back.fcz.domain.sanction.service;

import back.fcz.domain.sanction.constant.RiskLevel;
import back.fcz.domain.sanction.properties.SanctionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private SanctionProperties sanctionProperties;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate, sanctionProperties);
    }

    @Test
    void applyCooldown_회원_분단위_TTL로_expire_호출된다() {
        // when
        rateLimitService.applyCooldown(1L, 10);

        // then
        verify(redisTemplate).expire(
                eq("rate_limit:member:1"),
                eq(Duration.ofMinutes(10))
        );
    }

    @Test
    void applyCooldownByIp_IP_분단위_TTL로_expire_호출된다() {
        // when
        rateLimitService.applyCooldownByIp("127.0.0.1", 5);

        // then
        verify(redisTemplate).expire(
                eq("rate_limit:ip:127.0.0.1"),
                eq(Duration.ofMinutes(5))
        );
    }

    @Test
    void apply_윈도우내_요청수_초과하지_않으면_쿨다운_expire는_호출되지_않는다() {
        // given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(5L);

        SanctionProperties.RateLimit rateLimit = new SanctionProperties.RateLimit();
        rateLimit.setWindowSeconds(Map.of(RiskLevel.MEDIUM, 60));
        rateLimit.setMaxRequests(Map.of(RiskLevel.MEDIUM, 5));
        rateLimit.setCooldownSeconds(Map.of(RiskLevel.MEDIUM, 600));

        when(sanctionProperties.getRateLimit()).thenReturn(rateLimit);

        // when
        rateLimitService.apply(1L, RiskLevel.MEDIUM);

        // then
        verify(zSetOperations).removeRangeByScore(startsWith("rate_limit:member:1"), anyDouble(), anyDouble());
        verify(zSetOperations).add(startsWith("rate_limit:member:1"), anyString(), anyDouble());

        verify(redisTemplate).expire(
                eq("rate_limit:member:1"),
                eq(Duration.ofSeconds(60))
        );

        verify(redisTemplate, never()).expire(
                eq("rate_limit:member:1"),
                eq(Duration.ofSeconds(600))
        );
    }

    @Test
    void apply_요청수_초과하면_쿨다운_expire만_호출된다() {
        // given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(6L);

        SanctionProperties.RateLimit rateLimit = new SanctionProperties.RateLimit();
        rateLimit.setWindowSeconds(Map.of(RiskLevel.MEDIUM, 60));
        rateLimit.setMaxRequests(Map.of(RiskLevel.MEDIUM, 5));
        rateLimit.setCooldownSeconds(Map.of(RiskLevel.MEDIUM, 600));

        when(sanctionProperties.getRateLimit()).thenReturn(rateLimit);

        // when
        rateLimitService.apply(1L, RiskLevel.MEDIUM);

        // then
        verify(redisTemplate, times(1)).expire(
                eq("rate_limit:member:1"),
                eq(Duration.ofSeconds(600))
        );

        verify(redisTemplate, never()).expire(
                eq("rate_limit:member:1"),
                eq(Duration.ofSeconds(60))
        );
    }

    @Test
    void apply_Redis_장애가_나도_fail_open으로_예외가_외부로_전파되지_않는다() {
        // given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        doThrow(new RuntimeException("redis down"))
                .when(zSetOperations)
                .add(anyString(), anyString(), anyDouble());

        SanctionProperties.RateLimit rateLimit = new SanctionProperties.RateLimit();
        rateLimit.setWindowSeconds(Map.of(RiskLevel.MEDIUM, 60));
        rateLimit.setMaxRequests(Map.of(RiskLevel.MEDIUM, 5));
        rateLimit.setCooldownSeconds(Map.of(RiskLevel.MEDIUM, 600));

        when(sanctionProperties.getRateLimit()).thenReturn(rateLimit);

        // when & then
        assertDoesNotThrow(() ->
                rateLimitService.apply(1L, RiskLevel.MEDIUM)
        );
    }
}

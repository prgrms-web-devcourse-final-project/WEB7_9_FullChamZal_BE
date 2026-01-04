package back.fcz.domain.sanction.service;

import back.fcz.domain.sanction.constant.RiskLevel;
import back.fcz.domain.sanction.properties.SanctionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SanctionService sanctionService;

    @Mock
    private IpBlockService ipBlockService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private SanctionProperties sanctionProperties;

    @Mock
    private SanctionProperties.Monitoring monitoringProperties;

    @Mock
    private SanctionProperties.Monitoring.Thresholds thresholds;

    @InjectMocks
    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        given(sanctionProperties.getMonitoring()).willReturn(monitoringProperties);
        given(monitoringProperties.getThresholds()).willReturn(thresholds);
    }

    @Test
    void warning_threshold_초과시_rate_limit이_적용된다() {
        // given
        given(thresholds.getWarning()).willReturn(30);
        given(thresholds.getLimit()).willReturn(100);

        SanctionProperties.RateLimit rateLimit = new SanctionProperties.RateLimit();
        rateLimit.setCooldownSeconds(Map.of(RiskLevel.MEDIUM, 1800));

        given(sanctionProperties.getRateLimit()).willReturn(rateLimit);

        given(valueOperations.increment(anyString(), anyLong()))
                .willReturn(40L);

        // when
        monitoringService.incrementSuspicionScore(1L, 40);

        // then
        verify(rateLimitService).applyCooldown(eq(1L), anyInt());
        verify(sanctionService, never()).applyAutoSuspension(any(), any(), anyInt());
    }

    @Test
    void limit_threshold_초과시_자동제재가_발동된다() {
        // given
        given(thresholds.getLimit()).willReturn(50);

        given(valueOperations.increment(anyString(), anyLong()))
                .willReturn(60L);

        // when
        monitoringService.incrementSuspicionScore(1L, 60);

        // then
        verify(sanctionService).applyAutoSuspension(
                eq(1L),
                contains("의심 활동 누적"),
                anyInt()
        );

        verify(rateLimitService, never()).applyCooldown(anyLong(), anyInt());
    }

    @Test
    void 의심점수_증가시_매번_TTL이_갱신된다() {
        // given
        given(thresholds.getWarning()).willReturn(100);
        given(thresholds.getLimit()).willReturn(200);
        // getExpire 호출 제거 - 더 이상 TTL 체크 안 함
        given(valueOperations.increment(anyString(), anyLong()))
                .willReturn(10L);
        given(monitoringProperties.getSuspicionTtl())
                .willReturn(Duration.ofDays(7));

        // when
        monitoringService.incrementSuspicionScore(1L, 10);

        // then
        verify(redisTemplate).expire(
                contains("suspicion:member:1"),
                eq(Duration.ofDays(7))
        );
    }

    @Test
    void IP_warning_threshold_초과시_rate_limit이_적용된다() {
        // given
        given(thresholds.getWarning()).willReturn(30);
        given(thresholds.getLimit()).willReturn(100);

        SanctionProperties.RateLimit rateLimit = new SanctionProperties.RateLimit();
        rateLimit.setCooldownSeconds(Map.of(RiskLevel.MEDIUM, 1800));

        given(sanctionProperties.getRateLimit()).willReturn(rateLimit);

        given(valueOperations.increment(anyString(), anyLong()))
                .willReturn(35L);

        // when
        monitoringService.incrementSuspicionScoreByIp("127.0.0.1", 35);

        // then
        verify(rateLimitService).applyCooldownByIp(eq("127.0.0.1"), anyInt());
        verify(ipBlockService, never()).blockIp(anyString(), anyString());
    }

    @Test
    void IP_limit_threshold_초과시_IP차단이_발동된다() {
        // given
        given(thresholds.getLimit()).willReturn(50);

        given(valueOperations.increment(anyString(), anyLong()))
                .willReturn(80L);

        // when
        monitoringService.incrementSuspicionScoreByIp("127.0.0.1", 80);

        // then
        verify(ipBlockService).blockIp(
                eq("127.0.0.1"),
                contains("의심 활동 누적")
        );
    }
}

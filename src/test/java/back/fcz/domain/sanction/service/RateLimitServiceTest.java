package back.fcz.domain.sanction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate);
    }

    // ========== 회원 Rate Limit 테스트 ==========

    @Test
    @DisplayName("회원 - 기본 위험 레벨(0) - 첫 요청은 Rate Limit을 초과하지 않음")
    void isRateLimitExceeded_defaultLevel_firstRequest_notExceeded() {
        // Given
        Long memberId = 1L;
        int riskLevel = 0;

        // RedisTemplate의 opsForZSet 모킹
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // ZSet이 비어있음 (첫 요청)
        when(zSetOperations.zCard(anyString())).thenReturn(0L);
        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

        // When
        boolean result = rateLimitService.isRateLimitExceeded(memberId, riskLevel);

        // Then
        assertFalse(result, "첫 요청은 Rate Limit을 초과하지 않아야 함");

        // 슬라이딩 윈도우 정리가 호출되었는지 확인
        verify(zSetOperations).removeRangeByScore(anyString(), eq(0.0), anyDouble());

        // 새 요청이 추가되었는지 확인
        verify(zSetOperations).add(anyString(), anyString(), anyDouble());

        // TTL이 설정되었는지 확인 (윈도우의 2배)
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("회원 - 기본 위험 레벨(0) - 최대 요청 수 도달 시 Rate Limit 초과")
    void isRateLimitExceeded_defaultLevel_maxRequests_exceeded() {
        // Given
        Long memberId = 1L;
        int riskLevel = 0;

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // 기본 레벨은 1분에 20회
        when(zSetOperations.zCard(anyString())).thenReturn(20L);
        when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

        // When
        boolean result = rateLimitService.isRateLimitExceeded(memberId, riskLevel);

        // Then
        assertTrue(result, "최대 요청 수 도달 시 Rate Limit을 초과해야 함");

        // 새 요청이 추가되지 않았는지 확인
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("회원 - 의심 위험 레벨(1) - 엄격한 제한 적용 (10분에 5회)")
    void isRateLimitExceeded_suspiciousLevel_strictLimit() {
        // Given
        Long memberId = 1L;
        int riskLevel = 1;

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // 의심 레벨은 10분에 5회
        when(zSetOperations.zCard(anyString())).thenReturn(5L);
        when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

        // When
        boolean result = rateLimitService.isRateLimitExceeded(memberId, riskLevel);

        // Then
        assertTrue(result, "의심 레벨에서 5회 도달 시 Rate Limit을 초과해야 함");
    }

    @Test
    @DisplayName("회원 - 고위험 레벨(2) - 매우 엄격한 제한 적용 (10분에 3회)")
    void isRateLimitExceeded_highRiskLevel_veryStrictLimit() {
        // Given
        Long memberId = 1L;
        int riskLevel = 2;

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // 고위험 레벨은 10분에 3회
        when(zSetOperations.zCard(anyString())).thenReturn(3L);
        when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

        // When
        boolean result = rateLimitService.isRateLimitExceeded(memberId, riskLevel);

        // Then
        assertTrue(result, "고위험 레벨에서 3회 도달 시 Rate Limit을 초과해야 함");
    }

    @Test
    @DisplayName("회원 - Redis 장애 시 정상 접근 허용 (Fail-Open)")
    void isRateLimitExceeded_redisFailure_allowAccess() {
        // Given
        Long memberId = 1L;
        int riskLevel = 0;

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // Redis 예외 발생 시뮬레이션 - removeRangeByScore에서 예외 발생
        doThrow(new RuntimeException("Redis connection failed"))
                .when(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());

        // When
        boolean result = rateLimitService.isRateLimitExceeded(memberId, riskLevel);

        // Then
        assertFalse(result, "Redis 장애 시 정상 접근을 허용해야 함 (Fail-Open)");
    }

    // ========== IP 기반 Rate Limit 테스트 ==========

    @Test
    @DisplayName("IP - 기본 위험 레벨(0) - 첫 요청은 Rate Limit을 초과하지 않음")
    void isRateLimitExceededByIp_defaultLevel_firstRequest_notExceeded() {
        // Given
        String ipAddress = "192.168.1.1";
        int riskLevel = 0;

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        when(zSetOperations.zCard(anyString())).thenReturn(0L);
        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

        // When
        boolean result = rateLimitService.isRateLimitExceededByIp(ipAddress, riskLevel);

        // Then
        assertFalse(result, "IP 첫 요청은 Rate Limit을 초과하지 않아야 함");

        // IP 전용 키가 사용되었는지 확인
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(zSetOperations).removeRangeByScore(keyCaptor.capture(), anyDouble(), anyDouble());
        assertTrue(keyCaptor.getValue().contains("ratelimit:ip:"), "IP 전용 키를 사용해야 함");
    }

    @Test
    @DisplayName("IP - 최대 요청 수 도달 시 Rate Limit 초과")
    void isRateLimitExceededByIp_maxRequests_exceeded() {
        // Given
        String ipAddress = "192.168.1.1";
        int riskLevel = 0;

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        when(zSetOperations.zCard(anyString())).thenReturn(20L);
        when(zSetOperations.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);

        // When
        boolean result = rateLimitService.isRateLimitExceededByIp(ipAddress, riskLevel);

        // Then
        assertTrue(result, "IP 최대 요청 수 도달 시 Rate Limit을 초과해야 함");
    }

    // ========== 쿨다운 기능 테스트 ==========

    @Test
    @DisplayName("회원 - 쿨다운 적용 성공")
    void applyCooldown_member_success() {
        // Given
        Long memberId = 1L;
        int cooldownMinutes = 10;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        rateLimitService.applyCooldown(memberId, cooldownMinutes);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(
                keyCaptor.capture(),
                anyString(),
                durationCaptor.capture()
        );

        assertTrue(keyCaptor.getValue().contains(":cooldown"), "쿨다운 키가 사용되어야 함");
        assertEquals(Duration.ofMinutes(cooldownMinutes), durationCaptor.getValue(),
                "쿨다운 시간이 정확해야 함");
    }

    @Test
    @DisplayName("IP - 쿨다운 적용 성공")
    void applyCooldownByIp_success() {
        // Given
        String ipAddress = "192.168.1.1";
        int cooldownMinutes = 5;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        rateLimitService.applyCooldownByIp(ipAddress, cooldownMinutes);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(
                keyCaptor.capture(),
                anyString(),
                durationCaptor.capture()
        );

        assertTrue(keyCaptor.getValue().contains("ratelimit:ip:"), "IP 쿨다운 키가 사용되어야 함");
        assertTrue(keyCaptor.getValue().contains(":cooldown"), "쿨다운 키가 사용되어야 함");
        assertEquals(Duration.ofMinutes(cooldownMinutes), durationCaptor.getValue());
    }

    @Test
    @DisplayName("회원 - 쿨다운 중인지 확인 - 쿨다운 상태")
    void isInCooldown_member_inCooldown() {
        // Given
        Long memberId = 1L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("1234567890");

        // When
        boolean result = rateLimitService.isInCooldown(memberId);

        // Then
        assertTrue(result, "쿨다운 키가 존재하면 쿨다운 중이어야 함");
    }

    @Test
    @DisplayName("회원 - 쿨다운 중인지 확인 - 정상 상태")
    void isInCooldown_member_notInCooldown() {
        // Given
        Long memberId = 1L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        boolean result = rateLimitService.isInCooldown(memberId);

        // Then
        assertFalse(result, "쿨다운 키가 없으면 정상 상태여야 함");
    }

    @Test
    @DisplayName("IP - 쿨다운 중인지 확인 - 쿨다운 상태")
    void isInCooldownByIp_inCooldown() {
        // Given
        String ipAddress = "192.168.1.1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("1234567890");

        // When
        boolean result = rateLimitService.isInCooldownByIp(ipAddress);

        // Then
        assertTrue(result, "IP 쿨다운 키가 존재하면 쿨다운 중이어야 함");
    }

    @Test
    @DisplayName("회원 - 남은 쿨다운 시간 조회 - 정상")
    void getRemainingCooldown_member_hasRemaining() {
        // Given
        Long memberId = 1L;
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(300L);

        // When
        long remaining = rateLimitService.getRemainingCooldown(memberId);

        // Then
        assertEquals(300L, remaining, "남은 쿨다운 시간이 정확해야 함");
    }

    @Test
    @DisplayName("회원 - 남은 쿨다운 시간 조회 - 쿨다운 없음")
    void getRemainingCooldown_member_noCooldown() {
        // Given
        Long memberId = 1L;
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(-2L);

        // When
        long remaining = rateLimitService.getRemainingCooldown(memberId);

        // Then
        assertEquals(0L, remaining, "쿨다운이 없으면 0을 반환해야 함");
    }

    @Test
    @DisplayName("IP - 남은 쿨다운 시간 조회 - 정상")
    void getRemainingCooldownByIp_hasRemaining() {
        // Given
        String ipAddress = "192.168.1.1";
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(180L);

        // When
        long remaining = rateLimitService.getRemainingCooldownByIp(ipAddress);

        // Then
        assertEquals(180L, remaining, "IP 남은 쿨다운 시간이 정확해야 함");
    }

    // ========== Rate Limit 초기화 테스트 ==========

    @Test
    @DisplayName("회원 - Rate Limit 초기화 성공")
    void resetRateLimit_member_success() {
        // Given
        Long memberId = 1L;
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        rateLimitService.resetRateLimit(memberId);

        // Then
        // 2개의 키가 삭제되어야 함 (Rate Limit 키 + 쿨다운 키)
        verify(redisTemplate, times(2)).delete(anyString());
    }

    @Test
    @DisplayName("IP - Rate Limit 초기화 성공")
    void resetRateLimitByIp_success() {
        // Given
        String ipAddress = "192.168.1.1";
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        rateLimitService.resetRateLimitByIp(ipAddress);

        // Then
        // 2개의 키가 삭제되어야 함 (Rate Limit 키 + 쿨다운 키)
        verify(redisTemplate, times(2)).delete(anyString());
    }

    @Test
    @DisplayName("회원 - 쿨다운 적용 실패 시 예외 처리")
    void applyCooldown_member_exceptionHandling() {
        // Given
        Long memberId = 1L;
        int cooldownMinutes = 10;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis error"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // When & Then
        // 예외가 발생해도 메서드가 정상 종료되어야 함 (예외를 로그로만 기록)
        assertDoesNotThrow(() -> rateLimitService.applyCooldown(memberId, cooldownMinutes));
    }

    @Test
    @DisplayName("회원 - 쿨다운 확인 실패 시 예외 처리")
    void isInCooldown_member_exceptionHandling() {
        // Given
        Long memberId = 1L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        // When
        boolean result = rateLimitService.isInCooldown(memberId);

        // Then
        assertFalse(result, "Redis 장애 시 쿨다운 중이 아닌 것으로 처리 (Fail-Open)");
    }

    @Test
    @DisplayName("회원 - 남은 쿨다운 시간 조회 실패 시 예외 처리")
    void getRemainingCooldown_member_exceptionHandling() {
        // Given
        Long memberId = 1L;
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS)))
                .thenThrow(new RuntimeException("Redis error"));

        // When
        long remaining = rateLimitService.getRemainingCooldown(memberId);

        // Then
        assertEquals(0L, remaining, "Redis 장애 시 남은 시간은 0으로 처리");
    }

    @Test
    @DisplayName("회원 - Rate Limit 초기화 실패 시 예외 처리")
    void resetRateLimit_member_exceptionHandling() {
        // Given
        Long memberId = 1L;
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis error"));

        // When & Then
        // 예외가 발생해도 메서드가 정상 종료되어야 함
        assertDoesNotThrow(() -> rateLimitService.resetRateLimit(memberId));
    }
}
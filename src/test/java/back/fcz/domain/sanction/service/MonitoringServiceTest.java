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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SanctionService sanctionService;

    @Mock
    private IpBlockService ipBlockService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MonitoringService monitoringService;

    // 임계값 상수 (실제 서비스와 동일하게 유지)
    private static final int WARNING_THRESHOLD = 30;
    private static final int LIMIT_THRESHOLD = 50;
    private static final int BLOCK_THRESHOLD = 100;

    @BeforeEach
    void setUp() {
        monitoringService = new MonitoringService(
                redisTemplate,
                sanctionService,
                ipBlockService,
                rateLimitService
        );
    }

    // ========== 회원 의심 점수 증가 테스트 ==========

    @Test
    @DisplayName("회원 - 의심 점수 증가 성공")
    void incrementSuspicionScore_member_success() {
        // Given
        Long memberId = 1L;
        int scoreToAdd = 10;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(10L);
        when(valueOperations.get(anyString())).thenReturn("10");

        // When
        monitoringService.incrementSuspicionScore(memberId, scoreToAdd);

        // Then
        verify(valueOperations).increment(anyString(), eq(10L));
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("회원 - 경고 임계값(30) 도달 시 로그만 기록")
    void incrementSuspicionScore_member_warningThreshold() {
        // Given
        Long memberId = 1L;
        int scoreToAdd = 30;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(30L);
        when(valueOperations.get(anyString())).thenReturn("30");

        // When
        monitoringService.incrementSuspicionScore(memberId, scoreToAdd);

        // Then
        // 경고 수준에서는 제재가 적용되지 않음
        verify(sanctionService, never()).applyAutoSuspension(anyLong(), anyString(), anyInt());
        verify(rateLimitService, never()).applyCooldown(anyLong(), anyInt());
    }

    @Test
    @DisplayName("회원 - 제한 임계값(50) 도달 시 쿨다운 적용")
    void incrementSuspicionScore_member_limitThreshold() {
        // Given
        Long memberId = 1L;
        int scoreToAdd = 50;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(50L);
        when(valueOperations.get(anyString())).thenReturn("50");

        // When
        monitoringService.incrementSuspicionScore(memberId, scoreToAdd);

        // Then
        // 쿨다운이 적용되어야 함 (30분)
        verify(rateLimitService).applyCooldown(memberId, 30);

        // 아직 계정 정지는 아님
        verify(sanctionService, never()).applyAutoSuspension(anyLong(), anyString(), anyInt());
    }

    @Test
    @DisplayName("회원 - 차단 임계값(100) 도달 시 자동 정지 + 점수 초기화")
    void incrementSuspicionScore_member_blockThreshold() {
        // Given
        Long memberId = 1L;
        int scoreToAdd = 100;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(100L);
        when(valueOperations.get(anyString())).thenReturn("100");

        // When
        monitoringService.incrementSuspicionScore(memberId, scoreToAdd);

        // Then
        // 자동 정지가 적용되어야 함 (7일)
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(sanctionService).applyAutoSuspension(
                eq(memberId),
                reasonCaptor.capture(),
                eq(7)
        );

        assertTrue(reasonCaptor.getValue().contains("100점"), "사유에 점수가 포함되어야 함");

        // 제재 후 점수 초기화
        verify(redisTemplate).delete(anyString());
    }

    // ========== IP 의심 점수 증가 테스트 ==========

    @Test
    @DisplayName("IP - 의심 점수 증가 성공")
    void incrementSuspicionScoreByIp_success() {
        // Given
        String ipAddress = "192.168.1.1";
        int scoreToAdd = 10;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(10L);
        when(valueOperations.get(anyString())).thenReturn("10");

        // When
        monitoringService.incrementSuspicionScoreByIp(ipAddress, scoreToAdd);

        // Then
        verify(valueOperations).increment(anyString(), eq(10L));
        verify(redisTemplate).expire(anyString(), any(Duration.class));

        // IP 전용 키가 사용되었는지 확인
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).increment(keyCaptor.capture(), anyLong());
        assertTrue(keyCaptor.getValue().contains("suspicion:ip:"), "IP 전용 키를 사용해야 함");
    }

    @Test
    @DisplayName("IP - 경고 임계값(30) 도달 시 로그만 기록")
    void incrementSuspicionScoreByIp_warningThreshold() {
        // Given
        String ipAddress = "192.168.1.1";
        int scoreToAdd = 30;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(30L);
        when(valueOperations.get(anyString())).thenReturn("30");

        // When
        monitoringService.incrementSuspicionScoreByIp(ipAddress, scoreToAdd);

        // Then
        // IP 경고 수준에서는 제재가 적용되지 않음
        verify(ipBlockService, never()).blockIp(anyString(), anyString());
        verify(rateLimitService, never()).applyCooldownByIp(anyString(), anyInt());
    }

    @Test
    @DisplayName("IP - 제한 임계값(50) 도달 시 쿨다운 적용")
    void incrementSuspicionScoreByIp_limitThreshold() {
        // Given
        String ipAddress = "192.168.1.1";
        int scoreToAdd = 50;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(50L);
        when(valueOperations.get(anyString())).thenReturn("50");

        // When
        monitoringService.incrementSuspicionScoreByIp(ipAddress, scoreToAdd);

        // Then
        // IP 쿨다운이 적용되어야 함 (30분)
        verify(rateLimitService).applyCooldownByIp(ipAddress, 30);

        // 아직 IP 차단은 아님
        verify(ipBlockService, never()).blockIp(anyString(), anyString());
    }

    @Test
    @DisplayName("IP - 차단 임계값(100) 도달 시 IP 차단 + 점수 초기화")
    void incrementSuspicionScoreByIp_blockThreshold() {
        // Given
        String ipAddress = "192.168.1.1";
        int scoreToAdd = 100;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(100L);
        when(valueOperations.get(anyString())).thenReturn("100");

        // When
        monitoringService.incrementSuspicionScoreByIp(ipAddress, scoreToAdd);

        // Then
        // IP 차단이 적용되어야 함
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(ipBlockService).blockIp(
                eq(ipAddress),
                reasonCaptor.capture()
        );

        assertTrue(reasonCaptor.getValue().contains("100점"), "차단 사유에 점수가 포함되어야 함");

        // 차단 후 점수 초기화
        verify(redisTemplate).delete(anyString());
    }

    // ========== 의심 점수 조회 테스트 ==========

    @Test
    @DisplayName("회원 - 의심 점수 조회 성공")
    void getSuspicionScore_member_success() {
        // Given
        Long memberId = 1L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("45");

        // When
        int score = monitoringService.getSuspicionScore(memberId);

        // Then
        assertEquals(45, score, "조회된 점수가 정확해야 함");
    }

    @Test
    @DisplayName("회원 - 의심 점수 없을 때 0 반환")
    void getSuspicionScore_member_notExists() {
        // Given
        Long memberId = 1L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        int score = monitoringService.getSuspicionScore(memberId);

        // Then
        assertEquals(0, score, "점수가 없으면 0을 반환해야 함");
    }

    @Test
    @DisplayName("IP - 의심 점수 조회 성공")
    void getSuspicionScoreByIp_success() {
        // Given
        String ipAddress = "192.168.1.1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("60");

        // When
        int score = monitoringService.getSuspicionScoreByIp(ipAddress);

        // Then
        assertEquals(60, score, "IP 조회 점수가 정확해야 함");
    }

    @Test
    @DisplayName("IP - 의심 점수 없을 때 0 반환")
    void getSuspicionScoreByIp_notExists() {
        // Given
        String ipAddress = "192.168.1.1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        int score = monitoringService.getSuspicionScoreByIp(ipAddress);

        // Then
        assertEquals(0, score, "IP 점수가 없으면 0을 반환해야 함");
    }

    // ========== 의심 점수 초기화 테스트 ==========

    @Test
    @DisplayName("회원 - 의심 점수 초기화 성공")
    void resetSuspicionScore_member_success() {
        // Given
        Long memberId = 1L;
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        monitoringService.resetSuspicionScore(memberId);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).delete(keyCaptor.capture());
        assertTrue(keyCaptor.getValue().contains("suspicion:member:"),
                "회원 의심 점수 키를 삭제해야 함");
    }

    @Test
    @DisplayName("IP - 의심 점수 초기화 성공")
    void resetSuspicionScoreByIp_success() {
        // Given
        String ipAddress = "192.168.1.1";
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        monitoringService.resetSuspicionScoreByIp(ipAddress);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).delete(keyCaptor.capture());
        assertTrue(keyCaptor.getValue().contains("suspicion:ip:"),
                "IP 의심 점수 키를 삭제해야 함");
    }

    // ========== Redis 장애 예외 처리 테스트 ==========

    @Test
    @DisplayName("회원 - 점수 증가 시 Redis 장애 발생해도 서비스 계속 동작")
    void incrementSuspicionScore_member_redisFailure() {
        // Given
        Long memberId = 1L;
        int scoreToAdd = 10;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong()))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then
        // 예외가 발생해도 메서드가 정상 종료되어야 함
        assertDoesNotThrow(() -> monitoringService.incrementSuspicionScore(memberId, scoreToAdd));
    }

    @Test
    @DisplayName("회원 - 점수 조회 시 Redis 장애 발생하면 0 반환")
    void getSuspicionScore_member_redisFailure() {
        // Given
        Long memberId = 1L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // When
        int score = monitoringService.getSuspicionScore(memberId);

        // Then
        assertEquals(0, score, "Redis 장애 시 0을 반환해야 함");
    }

    @Test
    @DisplayName("회원 - 점수 초기화 시 Redis 장애 발생해도 예외 던지지 않음")
    void resetSuspicionScore_member_redisFailure() {
        // Given
        Long memberId = 1L;

        when(redisTemplate.delete(anyString()))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then
        assertDoesNotThrow(() -> monitoringService.resetSuspicionScore(memberId));
    }

    @Test
    @DisplayName("IP - 점수 증가 시 Redis 장애 발생해도 서비스 계속 동작")
    void incrementSuspicionScoreByIp_redisFailure() {
        // Given
        String ipAddress = "192.168.1.1";
        int scoreToAdd = 10;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong()))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then
        assertDoesNotThrow(() -> monitoringService.incrementSuspicionScoreByIp(ipAddress, scoreToAdd));
    }

    // ========== 누적 점수 시나리오 테스트 ==========

    @Test
    @DisplayName("회원 - 점수를 여러 번 증가시켜 임계값 도달")
    void incrementSuspicionScore_member_multipleIncreases() {
        // Given
        Long memberId = 1L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 첫 번째 증가: 20점 → 총 20점
        when(valueOperations.increment(anyString(), eq(20L))).thenReturn(20L);
        when(valueOperations.get(anyString())).thenReturn("20");
        monitoringService.incrementSuspicionScore(memberId, 20);

        // 두 번째 증가: 15점 → 총 35점 (경고 수준)
        when(valueOperations.increment(anyString(), eq(15L))).thenReturn(35L);
        when(valueOperations.get(anyString())).thenReturn("35");
        monitoringService.incrementSuspicionScore(memberId, 15);

        // 세 번째 증가: 20점 → 총 55점 (제한 수준)
        when(valueOperations.increment(anyString(), eq(20L))).thenReturn(55L);
        when(valueOperations.get(anyString())).thenReturn("55");
        monitoringService.incrementSuspicionScore(memberId, 20);

        // Then
        // 제한 수준에 도달했으므로 쿨다운이 적용되어야 함
        verify(rateLimitService, atLeastOnce()).applyCooldown(memberId, 30);
    }
}
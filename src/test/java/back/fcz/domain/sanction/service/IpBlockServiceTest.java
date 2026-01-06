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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpBlockServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private IpBlockService ipBlockService;

    @BeforeEach
    void setUp() {
        ipBlockService = new IpBlockService(redisTemplate);
    }

    // ========== IP 차단 테스트 ==========

    @Test
    @DisplayName("IP 차단 - 기본 기간(7일) 적용")
    void blockIp_defaultDuration() {
        // Given
        String ipAddress = "192.168.1.100";
        String reason = "의심 활동 누적";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        ipBlockService.blockIp(ipAddress, reason);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(
                keyCaptor.capture(),
                valueCaptor.capture(),
                durationCaptor.capture()
        );

        assertTrue(keyCaptor.getValue().contains("blocked:ip:"), "차단 IP 키를 사용해야 함");
        assertTrue(keyCaptor.getValue().contains(ipAddress), "IP 주소가 키에 포함되어야 함");
        assertTrue(valueCaptor.getValue().contains(reason), "사유가 값에 포함되어야 함");
        assertEquals(Duration.ofDays(7), durationCaptor.getValue(), "기본 기간은 7일이어야 함");
    }

    @Test
    @DisplayName("IP 차단 - 사용자 지정 기간 적용")
    void blockIp_customDuration() {
        // Given
        String ipAddress = "192.168.1.100";
        String reason = "심각한 위반";
        int durationDays = 30;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        ipBlockService.blockIp(ipAddress, reason, durationDays);

        // Then
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(
                anyString(),
                anyString(),
                durationCaptor.capture()
        );

        assertEquals(Duration.ofDays(30), durationCaptor.getValue(), "사용자 지정 기간이 적용되어야 함");
    }

    @Test
    @DisplayName("IP 차단 - 값에 타임스탬프 포함 확인")
    void blockIp_includesTimestamp() {
        // Given
        String ipAddress = "192.168.1.100";
        String reason = "스팸 행위";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        ipBlockService.blockIp(ipAddress, reason);

        // Then
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(valueOperations).set(
                anyString(),
                valueCaptor.capture(),
                any(Duration.class)
        );

        String value = valueCaptor.getValue();
        assertTrue(value.contains("|"), "사유와 타임스탬프가 구분자로 연결되어야 함");

        String[] parts = value.split("\\|");
        assertEquals(2, parts.length, "사유와 타임스탬프 두 부분으로 나뉘어야 함");
        assertEquals(reason, parts[0], "첫 번째 부분은 사유여야 함");

        // 타임스탬프 검증 (숫자로 파싱 가능한지 확인)
        assertDoesNotThrow(() -> Long.parseLong(parts[1]), "두 번째 부분은 타임스탬프여야 함");
    }

    // ========== IP 차단 여부 확인 테스트 ==========

    @Test
    @DisplayName("IP 차단 확인 - 차단된 IP")
    void isBlocked_blockedIp() {
        // Given
        String ipAddress = "192.168.1.100";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("의심 활동|1234567890");

        // When
        boolean result = ipBlockService.isBlocked(ipAddress);

        // Then
        assertTrue(result, "차단된 IP는 true를 반환해야 함");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).get(keyCaptor.capture());
        assertTrue(keyCaptor.getValue().contains("blocked:ip:"), "차단 IP 키를 사용해야 함");
    }

    @Test
    @DisplayName("IP 차단 확인 - 차단되지 않은 IP")
    void isBlocked_notBlockedIp() {
        // Given
        String ipAddress = "192.168.1.200";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        boolean result = ipBlockService.isBlocked(ipAddress);

        // Then
        assertFalse(result, "차단되지 않은 IP는 false를 반환해야 함");
    }

    @Test
    @DisplayName("IP 차단 확인 - Redis 장애 시 false 반환 (Fail-Open)")
    void isBlocked_redisFailure() {
        // Given
        String ipAddress = "192.168.1.100";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = ipBlockService.isBlocked(ipAddress);

        // Then
        assertFalse(result, "Redis 장애 시 false를 반환해야 함 (Fail-Open)");
    }

    // ========== IP 차단 해제 테스트 ==========

    @Test
    @DisplayName("IP 차단 해제 - 성공")
    void unblockIp_success() {
        // Given
        String ipAddress = "192.168.1.100";
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        ipBlockService.unblockIp(ipAddress);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).delete(keyCaptor.capture());

        assertTrue(keyCaptor.getValue().contains("blocked:ip:"), "차단 IP 키를 삭제해야 함");
        assertTrue(keyCaptor.getValue().contains(ipAddress), "IP 주소가 키에 포함되어야 함");
    }

    @Test
    @DisplayName("IP 차단 해제 - Redis 장애 시 예외 처리")
    void unblockIp_redisFailure() {
        // Given
        String ipAddress = "192.168.1.100";
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then
        // 예외가 발생해도 메서드가 정상 종료되어야 함
        assertDoesNotThrow(() -> ipBlockService.unblockIp(ipAddress));
    }

    // ========== 차단 사유 조회 테스트 ==========

    @Test
    @DisplayName("차단 사유 조회 - 성공")
    void getBlockReason_success() {
        // Given
        String ipAddress = "192.168.1.100";
        String expectedReason = "의심 활동 누적 (점수: 100점)";
        String storedValue = expectedReason + "|1234567890";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(storedValue);

        // When
        String reason = ipBlockService.getBlockReason(ipAddress);

        // Then
        assertEquals(expectedReason, reason, "저장된 사유를 정확히 반환해야 함");
    }

    @Test
    @DisplayName("차단 사유 조회 - 차단되지 않은 IP")
    void getBlockReason_notBlocked() {
        // Given
        String ipAddress = "192.168.1.200";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        String reason = ipBlockService.getBlockReason(ipAddress);

        // Then
        assertNull(reason, "차단되지 않은 IP는 null을 반환해야 함");
    }

    @Test
    @DisplayName("차단 사유 조회 - 구분자가 없는 경우 전체 값 반환")
    void getBlockReason_noDelimiter() {
        // Given
        String ipAddress = "192.168.1.100";
        String storedValue = "단순 사유";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(storedValue);

        // When
        String reason = ipBlockService.getBlockReason(ipAddress);

        // Then
        assertEquals(storedValue, reason, "구분자가 없으면 전체 값을 반환해야 함");
    }

    @Test
    @DisplayName("차단 사유 조회 - Redis 장애 시 null 반환")
    void getBlockReason_redisFailure() {
        // Given
        String ipAddress = "192.168.1.100";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        String reason = ipBlockService.getBlockReason(ipAddress);

        // Then
        assertNull(reason, "Redis 장애 시 null을 반환해야 함");
    }

    // ========== 통합 시나리오 테스트 ==========

    @Test
    @DisplayName("통합 시나리오 - IP 차단 후 확인 및 해제")
    void integrationScenario_blockCheckUnblock() {
        // Given
        String ipAddress = "192.168.1.100";
        String reason = "반복적인 실패 시도";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When - 차단
        ipBlockService.blockIp(ipAddress, reason);

        // Then - 차단 확인을 위해 모킹 설정
        when(valueOperations.get(anyString())).thenReturn(reason + "|1234567890");
        assertTrue(ipBlockService.isBlocked(ipAddress), "차단 후 isBlocked는 true를 반환해야 함");

        // When - 해제
        when(redisTemplate.delete(anyString())).thenReturn(true);
        ipBlockService.unblockIp(ipAddress);

        // Then - 해제 확인을 위해 모킹 재설정
        when(valueOperations.get(anyString())).thenReturn(null);
        assertFalse(ipBlockService.isBlocked(ipAddress), "해제 후 isBlocked는 false를 반환해야 함");
    }

    @Test
    @DisplayName("IP 차단 - 여러 IP 동시 차단")
    void blockIp_multipleIps() {
        // Given
        String ip1 = "192.168.1.100";
        String ip2 = "192.168.1.101";
        String ip3 = "192.168.1.102";
        String reason = "동일한 사유";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        ipBlockService.blockIp(ip1, reason);
        ipBlockService.blockIp(ip2, reason);
        ipBlockService.blockIp(ip3, reason);

        // Then
        // 각 IP마다 set이 호출되었는지 확인
        verify(valueOperations, times(3)).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("IP 차단 - Redis 장애 시에도 예외 던지지 않음")
    void blockIp_redisFailure() {
        // Given
        String ipAddress = "192.168.1.100";
        String reason = "테스트";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // When & Then
        // 예외가 발생해도 메서드가 정상 종료되어야 함
        assertDoesNotThrow(() -> ipBlockService.blockIp(ipAddress, reason));
    }
}
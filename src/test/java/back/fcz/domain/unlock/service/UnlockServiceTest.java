package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UnlockServiceTest {
    @Mock
    private CapsuleRepository capsuleRepository;

    @Spy
    @InjectMocks
    private UnlockService unlockService;

    // 테스트 캡슐 데이터
    private final long CAPSULE_ID = 1L;
    private final LocalDateTime CAPSULE_TIME = LocalDateTime.of(2025, 1, 1, 10, 0);
    private final double CAPSULE_LAT = 37.5665;  // 서울 시청 위도
    private final double CAPSULE_LNG = 126.9780; // 서울 시청 경도
    private final int RADIUS_M = 50;

    // --- 테스트용 엔티티 생성 메소드 ---
    private Capsule createTimeCapsule() {
        return Capsule.builder()
                .capsuleId(CAPSULE_ID)
                .uuid("test")
                .nickname("test")
                .content("test")
                .capsuleColor("RED")
                .capsulePackingColor("RED")
                .visibility("PRIVATE")
                .unlockType("TIME")
                .unlockAt(CAPSULE_TIME)
                .build();
    }

    private Capsule createLocationCapsule() {
        return Capsule.builder()
                .capsuleId(CAPSULE_ID)
                .uuid("test")
                .nickname("test")
                .content("test")
                .capsuleColor("RED")
                .capsulePackingColor("RED")
                .visibility("PRIVATE")
                .unlockType("LOCATION")
                .locationLat(CAPSULE_LAT)
                .locationLng(CAPSULE_LNG)
                .locationRadiusM(RADIUS_M)
                .build();
    }

    @Test
    @DisplayName("캡슐의 시간 해제 조건 <= 사용자의 현재 시간 이면 true를 반환해야 한다")
    void is_time_condition_met_true() {
        // given
        Capsule capsule = createTimeCapsule();
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 10, 5); // 캡슐 해제 시간 이후

        // when
        boolean result = unlockService.isTimeConditionMet(CAPSULE_ID, currentTime);

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("캡슐의 시간 해제 조건 > 사용자의 현재 시간 이면 false를 반환해야 한다")
    void is_time_condition_met_false() {
        // given
        Capsule capsule = createTimeCapsule();
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 9, 55); // 캡슐 해제 시간 이전

        // when
        boolean result = unlockService.isTimeConditionMet(CAPSULE_ID, currentTime);

        //then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("캡슐과 사용자 위치간 거리 ≤ 반경 이면 true를 반환해야 한다")
    void is_location_condition_met_true() {
        // given
        Capsule capsule = createLocationCapsule();
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));

        double currentLat = CAPSULE_LAT;
        double currentLng = CAPSULE_LNG;

        // when
        boolean result = unlockService.isLocationConditionMet(CAPSULE_ID, currentLat, currentLng);

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("캡슐과 사용자 위치간 거리 > 반경 이면 false를 반환해야 한다")
    void is_location_condition_met_false() {
        // given
        Capsule capsule = createLocationCapsule();
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));

        // 인천 공항 위도, 경도
        double currentLat = 37.4692;
        double currentLng = 126.451;

        // when
        boolean result = unlockService.isLocationConditionMet(CAPSULE_ID, currentLat, currentLng);

        //then
        assertThat(result).isFalse();
    }
}

package back.fcz.domain.unlock;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.unlock.service.UnlockService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UnlockServiceTest {

    @Autowired
    private CapsuleRepository capsuleRepository;
    @Autowired
    private UnlockService unlockService;

    // 테스트 캡슐 데이터
    private final LocalDateTime CAPSULE_TIME = LocalDateTime.of(2025, 1, 1, 10, 0);
    private final double CAPSULE_LAT = 37.5665;  // 서울 위도
    private final double CAPSULE_LNG = 126.9780; // 서울 경도
    private final int RADIUS_M = 50;

    @Test
    @DisplayName("캡슐의 시간 해제 조건 <= 사용자의 현재 시간 이면 true를 반환해야 한다")
    void is_time_condition_met_true() {
        // given
        // TODO: 캡슐 MVP 코드 개발 후, 수정이 필요할 수도 있음. capsuleColor, capsulePackingColor, visibility가 enum이 될 경우 에러 발생
        Capsule capsule = Capsule.builder()
                .uuid("test")
                .nickname("test")
                .content("test")
                .capsuleColor("test")
                .capsulePackingColor("test")
                .visibility("PRIVATE")
                .unlockType("TIME")
                .unlockAt(CAPSULE_TIME)
                .build();
        capsuleRepository.save(capsule);
        long capsuleId = capsule.getCapsuleId();

        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 10, 5);

        // when
        boolean result = unlockService.isTimeConditionMet(capsuleId, currentTime);

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("캡슐의 시간 해제 조건 > 사용자의 현재 시간 이면 false를 반환해야 한다")
    void is_time_condition_met_false() {
        // given
        // TODO: 캡슐 MVP 코드 개발 후, 수정이 필요할 수도 있음. capsuleColor, capsulePackingColor, visibility가 enum이 될 경우 에러 발생
        Capsule capsule = Capsule.builder()
                .uuid("test")
                .nickname("test")
                .content("test")
                .capsuleColor("test")
                .capsulePackingColor("test")
                .visibility("PRIVATE")
                .unlockType("TIME")
                .unlockAt(CAPSULE_TIME)
                .build();
        capsuleRepository.save(capsule);
        long capsuleId = capsule.getCapsuleId();

        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 9, 55);

        // when
        boolean result = unlockService.isTimeConditionMet(capsuleId, currentTime);

        //then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("캡슐과 사용자 위치간 거리 ≤ 반경 이면 true를 반환해야 한다")
    void is_location_condition_met_true() {
        // given
        // TODO: 캡슐 MVP 코드 개발 후, 수정이 필요할 수도 있음. capsuleColor, capsulePackingColor, visibility가 enum이 될 경우 에러 발생
        Capsule capsule = Capsule.builder()
                .uuid("test")
                .nickname("test")
                .content("test")
                .capsuleColor("test")
                .capsulePackingColor("test")
                .visibility("PRIVATE")
                .unlockType("LOCATION")
                .locationLat(CAPSULE_LAT)
                .locationLng(CAPSULE_LNG)
                .locationRadiusM(RADIUS_M)
                .build();
        capsuleRepository.save(capsule);
        long capsuleId = capsule.getCapsuleId();

        double currentLat = CAPSULE_LAT + 0.00003;
        double currentLng = CAPSULE_LNG + 0.00003;

        // when
        boolean result = unlockService.isLocationConditionMet(capsuleId, currentLat, currentLng);

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("캡슐과 사용자 위치간 거리 > 반경 이면 false를 반환해야 한다")
    void is_location_condition_met_false() {
        // given
        // TODO: 캡슐 MVP 코드 개발 후, 수정이 필요할 수도 있음. capsuleColor, capsulePackingColor, visibility가 enum이 될 경우 에러 발생
        Capsule capsule = Capsule.builder()
                .uuid("test")
                .nickname("test")
                .content("test")
                .capsuleColor("test")
                .capsulePackingColor("test")
                .visibility("PRIVATE")
                .unlockType("LOCATION")
                .locationLat(CAPSULE_LAT)
                .locationLng(CAPSULE_LNG)
                .locationRadiusM(RADIUS_M)
                .build();
        capsuleRepository.save(capsule);
        long capsuleId = capsule.getCapsuleId();

        // 인천 공항 위도, 경도
        double currentLat = 37.4692;
        double currentLng = 126.451;

        // when
        boolean result = unlockService.isLocationConditionMet(capsuleId, currentLat, currentLng);

        //then
        assertThat(result).isFalse();
    }
}

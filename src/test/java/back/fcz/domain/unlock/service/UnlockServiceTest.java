package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private final LocalDateTime CAPSULE_UNLOCK_AT = LocalDateTime.of(2025, 1, 1, 10, 0);
    private final double CAPSULE_LAT = 37.5665;  // 서울 시청 위도
    private final double CAPSULE_LNG = 126.9780; // 서울 시청 경도
    private final int RADIUS_M = 50;

    // --- 테스트용 엔티티 생성 메소드 ---
    private Capsule createTimeCapsule(LocalDateTime capsuleUnlockUntil) {
        return Capsule.builder()
                .capsuleId(CAPSULE_ID)
                .uuid("test")
                .nickname("test")
                .content("test")
                .capsuleColor("RED")
                .capsulePackingColor("RED")
                .visibility("PRIVATE")
                .unlockType("TIME")
                .unlockAt(CAPSULE_UNLOCK_AT)
                .unlockUntil(capsuleUnlockUntil)
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
    @DisplayName("캡슐 열람 마감 시간이 없고, 캡슐의 시간 해제 조건 <= 사용자의 현재 시간 이면 true를 반환해야 한다")
    void no_unlockUntil_and_isTimeConditionMet_true() {
        // given
        Capsule capsule = createTimeCapsule(null);
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 10, 5); // 캡슐 해제 시간 이후

        // when
        boolean result = unlockService.isTimeConditionMet(CAPSULE_ID, currentTime);

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("캡슐 열람 마감 시간이 없고, 캡슐의 시간 해제 조건 > 사용자의 현재 시간 이면 false를 반환해야 한다")
    void no_unlockUntil_and_isTimeConditionMet_false() {
        // given
        Capsule capsule = createTimeCapsule(null);
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 9, 55); // 캡슐 해제 시간 이전

        // when
        boolean result = unlockService.isTimeConditionMet(CAPSULE_ID, currentTime);

        //then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("캡슐의 시간 해제 조건 <= 사용자의 현재 시간 <= 캡슐 열람 마감 시간 이면 true를 반환해야 한다")
    void have_unlockUntil_and_isTimeConditionMet_true() {
        // given
        Capsule capsule = createTimeCapsule(LocalDateTime.of(2025, 1, 1, 11, 0));
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 10, 5);

        // when
        boolean result = unlockService.isTimeConditionMet(CAPSULE_ID, currentTime);

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("캡슐 열람 마감 시간 > 사용자의 현재 시간 이면 false를 반환해야 한다")
    void have_unlockUntil_and_isTimeConditionMet_false() {
        // given
        Capsule capsule = createTimeCapsule(LocalDateTime.of(2025, 1, 1, 11, 0));
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 11, 5);

        // when
        boolean result = unlockService.isTimeConditionMet(CAPSULE_ID, currentTime);

        //then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("현재 시간이 unlockAt과 정확히 같으면 true를 반환해야 한다 (경계 조건)")
    void isTimeConditionMet_boundary_at_equal() {
        // given
        Capsule capsule = createTimeCapsule(null);
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));
        LocalDateTime currentTime = CAPSULE_UNLOCK_AT;

        // when
        boolean result = unlockService.isTimeConditionMet(CAPSULE_ID, currentTime);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("현재 시간이 unlockUntil과 정확히 같으면 true를 반환해야 한다 (경계 조건)")
    void isTimeConditionMet_boundary_until_equal() {
        // given
        LocalDateTime unlockUntil = LocalDateTime.of(2025, 1, 1, 11, 0);
        Capsule capsule = createTimeCapsule(unlockUntil);
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));
        LocalDateTime currentTime = unlockUntil;

        // when
        boolean result = unlockService.isTimeConditionMet(CAPSULE_ID, currentTime);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("unlockUntil <= unlockAt 이면, INVALID_UNLOCK_TIME_RANGE 예외 발생")
    void isTimeConditionMet_throws_exception_when_unlockUntil_before_or_equal_unlockAt() {
        // given
        Capsule capsule = createTimeCapsule(CAPSULE_UNLOCK_AT);
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));
        LocalDateTime currentTime = LocalDateTime.now();

        // when
        BusinessException exception = assertThrows(BusinessException.class, () ->
                unlockService.isTimeConditionMet(CAPSULE_ID, currentTime)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_UNLOCK_TIME_RANGE);
    }

    @Test
    @DisplayName("unlockAt이 null일 경우, UNLOCK_TIME_NOT_FOUND 예외 발생")
    void isTimeConditionMet_throws_exception_when_unlockAt_not_exist() {
        // given
        Capsule capsule = Capsule.builder()
                .capsuleId(CAPSULE_ID)
                .uuid("test")
                .nickname("test")
                .content("test")
                .capsuleColor("RED")
                .capsulePackingColor("RED")
                .visibility("PRIVATE")
                .unlockType("TIME")
                .build();
        when(capsuleRepository.findById(CAPSULE_ID)).thenReturn(Optional.of(capsule));
        LocalDateTime currentTime = LocalDateTime.now();

        // when
        BusinessException exception = assertThrows(BusinessException.class, () ->
                unlockService.isTimeConditionMet(CAPSULE_ID, currentTime)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNLOCK_TIME_NOT_FOUND);
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

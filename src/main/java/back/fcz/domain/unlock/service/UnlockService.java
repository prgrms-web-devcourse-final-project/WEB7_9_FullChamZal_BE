package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.unlock.dto.response.projection.NearbyOpenCapsuleProjection;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UnlockService {
    private static final double EARTH_RADIUS_M = 6371000; // 지구 반지름 (m)
    private final CapsuleRepository capsuleRepository;
    private final FirstComeService firstComeService;

    // 개인 캡슐
    public boolean validateUnlockConditionsForPrivate(
            Capsule capsule,
            LocalDateTime currentTime,
            Double currentLat,
            Double currentLng
    ) {
        return validateTimeAndLocationConditions(capsule, currentTime, currentLat, currentLng);
    }

    // 시간/위치 조건만 검증 (선착순 제외)
    public boolean validateTimeAndLocationConditions(
            Capsule capsule,
            LocalDateTime currentTime,
            Double currentLat,
            Double currentLng
    ) {
        String unlockType = capsule.getUnlockType();

        return switch (unlockType) {
            case "TIME" -> isTimeConditionMet(capsule.getCapsuleId(), currentTime);
            case "LOCATION" -> isLocationConditionMet(capsule.getCapsuleId(), currentLat, currentLng);
            case "TIME_AND_LOCATION" -> isTimeAndLocationConditionMet(capsule.getCapsuleId(), currentTime, currentLat, currentLng);
            default -> throw new BusinessException(ErrorCode.CAPSULE_CONDITION_ERROR);
        };
    }

    // 사용자 근처 공개 캡슐 조회 전용 위치/위치+시간 조건 검증
    public boolean validateNearbyCapsuleConditions(
            NearbyOpenCapsuleProjection capsule,
            LocalDateTime currentTime,
            Double currentLat,
            Double currentLng
    ) {
        String unlockType = capsule.unlockType();

        return switch (unlockType) {
            case "LOCATION" -> isLocationConditionMet(capsule.capsuleId(), currentLat, currentLng);
            case "TIME_AND_LOCATION" -> isTimeAndLocationConditionMet(capsule.capsuleId(), currentTime, currentLat, currentLng);
            default -> throw new BusinessException(ErrorCode.CAPSULE_CONDITION_ERROR);
        };
    }

    /*
    시간 해제 조건 검증

    시간 해제 조건을 사용하는 캡슐은 다음 두 가지 유형으로 나뉜다.
    1. unlockAt만 설정된 경우
       - 지정된 시점(unlockAt)이 되면 캡슐이 해제된다.
    2. unlockAt과 unlockUntil이 모두 설정된 경우
       - unlockAt부터 unlockUntil 사이의 기간 동안만 캡슐 해제가 가능하다.
     */
    public boolean isTimeConditionMet(long capsuleId, LocalDateTime currentTime) {
        if(currentTime == null) {throw new BusinessException(ErrorCode.INVALID_UNLOCK_TIME);}

        Capsule capsule = capsuleRepository.findById(capsuleId).orElseThrow(
                () -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));
        LocalDateTime capsuleUnlockAt = capsule.getUnlockAt();
        LocalDateTime capsuleUnlockUntil = capsule.getUnlockUntil();

        if(capsuleUnlockAt == null) {throw new BusinessException(ErrorCode.UNLOCK_TIME_NOT_FOUND);}
        // unlockUntil <= unlockAt 이면 error
        if (capsuleUnlockAt != null && capsuleUnlockUntil != null) {
            if (!capsuleUnlockUntil.isAfter(capsuleUnlockAt)) {throw new BusinessException(ErrorCode.INVALID_UNLOCK_TIME_RANGE);}
        }

        // 캡슐의 시간 해제 조건 <= 사용자의 현재 시간 이면 true
        boolean isAfterOrEqualToUnlockAt = !capsuleUnlockAt.isAfter(currentTime);

        boolean isBeforeOrEqualToUnlockUntil;
        if (capsuleUnlockUntil == null) {
            // null인 경우, 열람 마감 시간이 없는 캡슐이므로 항상 true
            isBeforeOrEqualToUnlockUntil = true;
        } else {
            // 사용자의 현재 시간 <= 캡슐의 열람 마감 시간 이면 true
            isBeforeOrEqualToUnlockUntil = !capsuleUnlockUntil.isBefore(currentTime);
        }

        // unlockAt <= 사용자의 현재 시간 <= unlockUntil 이면 true (해제 가능)
        return isAfterOrEqualToUnlockAt && isBeforeOrEqualToUnlockUntil;
    }

    // 위치 해제 조건 검증
    public boolean isLocationConditionMet(long capsuleId, double currentLat, double currentLng) {
        if(currentLat < -90 || currentLat > 90 || currentLng < -180 || currentLng > 180) {
            throw new BusinessException(ErrorCode.INVALID_LATITUDE_LONGITUDE);
        }

        Capsule capsule = capsuleRepository.findById(capsuleId).orElseThrow(
                () -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));
        double capsuleLat = capsule.getLocationLat();
        double capsuleLng = capsule.getLocationLng();
        int radiusM = capsule.getLocationRadiusM();

        double distance = calculateDistanceInMeters(capsuleLat, capsuleLng, currentLat, currentLng);

        // 계산된 거리 ≤ 반경 이면 true (해제 가능)
        return distance <= radiusM;
    }

    // 시간 + 위치 해제 조건 검증
    public boolean isTimeAndLocationConditionMet(long capsuleId, LocalDateTime currentTime, double currentLat, double currentLng) {
        boolean isTimeConditionMet = isTimeConditionMet(capsuleId, currentTime);
        boolean isLocationConditionMet = isLocationConditionMet(capsuleId, currentLat, currentLng);

        return isTimeConditionMet && isLocationConditionMet;
    }

    // 사용자 현재 위치와 캡슐 위치 조건 간 거리 계산 (Haversine Formula)
    public double calculateDistanceInMeters(double capsuleLat, double capsuleLng, double currentLat, double currentLng) {

        double deltaLat = Math.toRadians(currentLat - capsuleLat);
        double deltaLng = Math.toRadians(currentLng - capsuleLng);
        double squareRoot = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(Math.toRadians(capsuleLat)) * Math.cos(Math.toRadians(currentLat)) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double distance = 2 * EARTH_RADIUS_M * Math.atan2(Math.sqrt(squareRoot), Math.sqrt(1 - squareRoot));

        return distance;
    }
}

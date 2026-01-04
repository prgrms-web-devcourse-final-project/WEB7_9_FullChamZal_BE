package back.fcz.domain.unlock.service;

import back.fcz.domain.capsule.entity.AnomalyType;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleOpenLog;
import back.fcz.domain.capsule.repository.CapsuleOpenLogRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.sanction.constant.SanctionConstants;
import back.fcz.domain.sanction.util.AnomalyDetector;
import back.fcz.domain.unlock.dto.UnlockValidationResult;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnlockService {
    private static final double EARTH_RADIUS_M = 6371000; // 지구 반지름 (m)
    private final CapsuleRepository capsuleRepository;
    private final CapsuleOpenLogRepository capsuleOpenLogRepository;
    private final SanctionConstants sanctionConstants;

    // 개인 캡슐
    public UnlockValidationResult validateUnlockConditionsForPrivate(
            Capsule capsule,
            LocalDateTime currentTime,
            Double currentLat,
            Double currentLng,
            LocalDateTime clientTime,
            Long memberId,
            String ipAddress
    ) {
        return validateTimeAndLocationConditions(
                capsule, currentTime, currentLat, currentLng, clientTime, memberId, ipAddress
        );
    }

    // 시간/위치 조건만 검증 (선착순 제외)
    public UnlockValidationResult validateTimeAndLocationConditions(
            Capsule capsule,
            LocalDateTime currentTime,
            Double currentLat,
            Double currentLng,
            LocalDateTime clientTime,
            Long memberId,
            String ipAddress
    ) {
        String unlockType = capsule.getUnlockType();

        if ((unlockType.equals("LOCATION") || unlockType.equals("TIME_AND_LOCATION"))) {
            if (currentLat == null || currentLng == null) {
                throw new BusinessException(ErrorCode.LOCATION_REQUIRED);
            }
        }

        // 1. 기본 조건 검증
        boolean conditionMet = switch (unlockType) {
            case "TIME" -> isTimeConditionMet(capsule, currentTime);
            case "LOCATION" -> isLocationConditionMet(capsule, currentLat, currentLng);
            case "TIME_AND_LOCATION" -> isTimeAndLocationConditionMet(capsule, currentTime, currentLat, currentLng);
            default -> throw new BusinessException(ErrorCode.CAPSULE_CONDITION_ERROR);
        };

        // 2. 이상 활동 감지
        AnomalyType anomalyType = detectAnomaly(
                capsule, currentLat, currentLng, currentTime, clientTime, memberId, ipAddress
        );

        // 3. 의심 점수 계산
        int suspicionScore = sanctionConstants.getScoreByAnomaly(anomalyType);

        // 4. 결과 반환
        if (anomalyType != AnomalyType.NONE) {
            return UnlockValidationResult.anomalyDetected(conditionMet, anomalyType, suspicionScore);
        }

        if (conditionMet) {
            return UnlockValidationResult.success();
        }

        return UnlockValidationResult.conditionFailed();
    }

    // 사용자 근처 공개 캡슐 조회 전용 위치/위치+시간 조건 검증
    public boolean validateNearbyCapsuleConditions(
            Capsule capsule,
            LocalDateTime currentTime,
            Double currentLat,
            Double currentLng
    ) {
        String unlockType = capsule.getUnlockType();

        return switch (unlockType) {
            case "LOCATION" -> isLocationConditionMet(capsule, currentLat, currentLng);
            case "TIME_AND_LOCATION" -> isTimeAndLocationConditionMet(capsule, currentTime, currentLat, currentLng);
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
    public boolean isTimeConditionMet(Capsule capsule, LocalDateTime currentTime) {
        if(currentTime == null) {throw new BusinessException(ErrorCode.INVALID_UNLOCK_TIME);}

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
    public boolean isLocationConditionMet(Capsule capsule, double currentLat, double currentLng) {
        if(currentLat < -90 || currentLat > 90 || currentLng < -180 || currentLng > 180) {
            throw new BusinessException(ErrorCode.INVALID_LATITUDE_LONGITUDE);
        }

        double capsuleLat = capsule.getLocationLat();
        double capsuleLng = capsule.getLocationLng();
        int radiusM = capsule.getLocationRadiusM();

        double distance = calculateDistanceInMeters(capsuleLat, capsuleLng, currentLat, currentLng);

        // 계산된 거리 ≤ 반경 이면 true (해제 가능)
        return distance <= radiusM;
    }

    // 시간 + 위치 해제 조건 검증
    public boolean isTimeAndLocationConditionMet(Capsule capsule, LocalDateTime currentTime, double currentLat, double currentLng) {
        boolean isTimeConditionMet = isTimeConditionMet(capsule, currentTime);
        boolean isLocationConditionMet = isLocationConditionMet(capsule, currentLat, currentLng);

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

    // 이상 활동 감지 로직
    private AnomalyType detectAnomaly(
            Capsule capsule,
            Double currentLat,
            Double currentLng,
            LocalDateTime serverTime,
            LocalDateTime clientTime,
            Long memberId,
            String ipAddress
    ) {
        // 1. 좌표 유효성 검증 (위치 정보가 있는 경우만)
        if (currentLat != null && currentLng != null) {
            if (!AnomalyDetector.isValidCoordinate(currentLat, currentLng)) {
                return AnomalyType.SUSPICIOUS_PATTERN;
            }
        }

        // 2. 시간 조작 감지 (위치 정보 없어도 가능)
        if (AnomalyDetector.isTimeManipulation(serverTime, clientTime)) {
            return AnomalyType.TIME_MANIPULATION;
        }

        // 3. 이전 로그 조회 (회원/비회원 분기)
        List<CapsuleOpenLog> recentLogs;

        if (memberId != null) {
            // 회원: memberId로 조회
            recentLogs = capsuleOpenLogRepository
                    .findTop15ByCapsuleId_CapsuleIdAndMemberId_MemberIdOrderByOpenedAtDesc(
                            capsule.getCapsuleId(),
                            memberId
                    );
        } else if (ipAddress != null && !ipAddress.equals("UNKNOWN")) {
            // 비회원: IP로 조회
            recentLogs = capsuleOpenLogRepository
                    .findTop15ByCapsuleId_CapsuleIdAndIpAddressOrderByOpenedAtDesc(
                            capsule.getCapsuleId(),
                            ipAddress
                    );
        } else {
            // IP도 없으면 로그 조회 불가
            recentLogs = List.of();
        }

        // 첫 시도면 패턴 분석 불가
        if (recentLogs.isEmpty()) {
            return AnomalyType.NONE;
        }

        // 4. 불가능한 이동 감지 (위치 정보가 있는 경우만)
        if (currentLat != null && currentLng != null) {
            Optional<CapsuleOpenLog> lastValidLog = recentLogs.stream()
                    .filter(log -> log.getCurrentLat() != null && log.getCurrentLng() != null)
                    .findFirst();

            if (lastValidLog.isPresent()) {
                CapsuleOpenLog baseLog = lastValidLog.get();

                int movementLevel = AnomalyDetector.classifyMovementAnomaly(
                        baseLog.getCurrentLat(), baseLog.getCurrentLng(),
                        currentLat, currentLng,
                        baseLog.getOpenedAt(), serverTime
                );

                if (movementLevel >= 3) {
                    return AnomalyType.IMPOSSIBLE_MOVEMENT;
                }

                if (movementLevel >= 2) {
                    return AnomalyType.SUSPICIOUS_PATTERN;
                }
                else if (movementLevel == 1) {
                    log.info("의심 이동 감지 (점수 미적용): memberId={}", memberId);
                }
            }
        }

        // 5. 짧은 시간 내 반복 시도 (위치 정보 없어도 가능)
        long recentAttemptsCount = recentLogs.stream()
                .filter(log -> log.getOpenedAt().isAfter(serverTime.minusMinutes(5)))
                .count();

        if (recentAttemptsCount >= 7) {
            return AnomalyType.RAPID_RETRY;
        }

        // 6. 조건 반복 실패 (위치 정보 없어도 가능)
        if (recentLogs.size() >= 15) {
            long failedCount = recentLogs.stream()
                    .limit(15)
                    .filter(log -> "FAILED".equals(log.getStatus().name()))
                    .count();

            if (failedCount >= 15) {
                return AnomalyType.LOCATION_RETRY;
            }
        }

        return AnomalyType.NONE;
    }
}

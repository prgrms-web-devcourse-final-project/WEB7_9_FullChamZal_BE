package back.fcz.domain.sanction.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
public class AnomalyDetector {

    // 지구 반지름 (km)
    private static final double EARTH_RADIUS_KM = 6371.0;

    // 3단계 속도 임계값
    private static final double EXTREME_SPEED_KMH = 1000.0;  // 즉시 차단
    private static final double HIGH_SPEED_KMH = 300.0;      // 강한 의심
    private static final double SUSPICIOUS_SPEED_KMH = 150.0; // 누적 의심

    // 시간 간격 기준 (초)
    private static final long SHORT_INTERVAL_SEC = 300;   // 5분
    private static final long MEDIUM_INTERVAL_SEC = 3600; // 1시간

    // 시간 동기화 허용 오차 (분)
    private static final int TIME_SYNC_TOLERANCE_MINUTES = 10;

    /**
     * Haversine 공식을 사용하여 두 GPS 좌표 간의 거리 계산
     * @return 두 지점 간의 거리 (km)
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // 위도/경도를 라디안으로 변환
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * 두 위치 간 이동 속도를 계산 (시속 km)
     * @return 이동 속도 (km/h), 시간이 0 이하면 0.0 반환
     */
    public static double calculateSpeed(double distance, long timeDiffSeconds) {
        if (timeDiffSeconds <= 0) {
            if (distance >= 0.001) {  // 1m 이상 이동
                return Double.MAX_VALUE;
            }

            return 0.0;
        }

        double hours = timeDiffSeconds / 3600.0;
        // 속도(km/h) 반환
        return distance / hours;
    }

    /**
     * 이동 속도를 3단계로 분류하여 의심 수준 반환
     * @return 0: 정상, 1: 누적의심(150+), 2: 강한의심(300+), 3: 즉시차단(1000+)
     */
    public static int classifyMovementAnomaly(
            double previousLat, double previousLng,
            double currentLat, double currentLng,
            LocalDateTime previousTime, LocalDateTime currentTime) {
        long timeDiffSeconds = Duration.between(previousTime, currentTime).getSeconds();
        double distance = calculateDistance(previousLat, previousLng, currentLat, currentLng);

        // 시간 역행 (클라이언트 조작)
        if (timeDiffSeconds < 0) {
            // GPS 오차 범위(100m) 내는 허용
            if (distance < 0.1) {
                log.debug("시간 역행이지만 GPS 오차 범위 내: {}초, {}km", timeDiffSeconds, distance);
                return 0;
            }

            log.warn("시간 역행 + 위치 변경 감지: {}초, {}km", timeDiffSeconds, distance);
            return 3;
        }

        // 정확히 동일 시간 (0초)
        if (timeDiffSeconds == 0) {
            // 200m 미만은 GPS 재연결 오차로 간주
            if (distance < 0.2) {
                log.debug("동일 시간이지만 GPS 재연결 오차 범위 내: {}km", distance);
                return 0;
            }

            log.warn("동일 시간 위치 변경 감지: {}km 이동", distance);
            return 3;
        }

        // 일반 GPS 오차(5-10m) + 실내 오차(~50m) 고려
        if (distance < 0.1) {  // 100m
            log.debug("이동 거리 {}km는 GPS 오차 범위 내로 판단", distance);
            return 0;
        }

        // 짧은 시간 + 짧은 거리는 추가로 관대하게
        if (timeDiffSeconds < 60 && distance < 0.2) {  // 1분 & 200m
            log.debug("짧은 시간 이동({}초, {}km)은 정상으로 판단", timeDiffSeconds, distance);
            return 0;
        }

        double speed = calculateSpeed(distance, timeDiffSeconds);

        // 시간 간격에 따라 임계값 조정
        double adjustedExtremeThreshold = EXTREME_SPEED_KMH;
        double adjustedHighThreshold = HIGH_SPEED_KMH;
        double adjustedSuspiciousThreshold = SUSPICIOUS_SPEED_KMH;

        if (timeDiffSeconds < 300) {  // 5분 미만
            adjustedSuspiciousThreshold = 80.0;
        } else if (timeDiffSeconds < 600) {  // 10분 미만
            adjustedSuspiciousThreshold = 100.0;
        } else if (timeDiffSeconds < 1800) {  // 30분 미만
            adjustedSuspiciousThreshold = 120.0;
        } else if (timeDiffSeconds < 3600) {  // 1시간 미만
            adjustedSuspiciousThreshold = 140.0;
        } else {  // 1시간 이상
            adjustedSuspiciousThreshold = 150.0;
        }

        log.debug("이동 분석 - 거리: {}km, 시간: {}초, 속도: {}km/h",
                distance, timeDiffSeconds, speed);

        if (speed >= adjustedExtremeThreshold) {
            log.warn("즉시 차단급 이동 감지: {}km/h", speed);
            return 3;
        } else if (speed >= adjustedHighThreshold) {
            log.warn("강한 의심 이동 감지: {}km/h", speed);
            return 2;
        } else if (speed >= adjustedSuspiciousThreshold) {
            log.info("의심 이동 감지: {}km/h", speed);
            return 1;
        }

        return 0;
    }

    /**
     * 서버 시간과 클라이언트 시간의 차이가 허용 범위 내인지 검증
     * 클라이언트가 시간을 조작했을 가능성을 감지
     * @return 시간 조작 의심이면 true, 아니면 false
     */
    public static boolean isTimeManipulation(LocalDateTime serverTime, LocalDateTime clientTime) {
        if (clientTime == null) {
            return false; // 클라이언트 시간이 없으면 검증 불가
        }

        long diffMinutes = Math.abs(Duration.between(serverTime, clientTime).toMinutes());

        log.debug("서버-클라이언트 시간 차이: {}분", diffMinutes);

        return diffMinutes > TIME_SYNC_TOLERANCE_MINUTES;
    }

    /**
     * GPS 좌표의 유효성을 검증
     * @return 유효하면 true, 아니면 false
     */
    public static boolean isValidCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }

        // 위도: -90 ~ 90, 경도: -180 ~ 180
        return latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }
}

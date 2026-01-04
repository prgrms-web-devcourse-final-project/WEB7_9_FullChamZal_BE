package back.fcz.global.util;

import back.fcz.domain.sanction.util.AnomalyDetector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("AnomalyDetector 테스트")
class AnomalyDetectorTest {

    @Test
    @DisplayName("Haversine 공식으로 서울-부산 거리를 계산한다")
    void calculateDistanceSeoulToBusan() {
        // given
        double seoulLat = 37.5665;
        double seoulLng = 126.9780;
        double busanLat = 35.1796;
        double busanLng = 129.0756;

        // when
        double distance = AnomalyDetector.calculateDistance(seoulLat, seoulLng, busanLat, busanLng);

        // then
        // 서울-부산 직선거리는 약 325km
        assertThat(distance).isCloseTo(325.0, within(10.0));
    }

    @Test
    @DisplayName("동일한 위치의 거리는 0이다")
    void calculateDistanceSameLocation() {
        // given
        double lat = 37.5665;
        double lng = 126.9780;

        // when
        double distance = AnomalyDetector.calculateDistance(lat, lng, lat, lng);

        // then
        assertThat(distance).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("이동 속도를 계산한다 (100km를 1시간에 이동)")
    void calculateSpeed100KmPerHour() {
        // given
        double distance = 100.0; // km
        long timeDiffSeconds = 3600; // 1시간

        // when
        double speed = AnomalyDetector.calculateSpeed(distance, timeDiffSeconds);

        // then
        assertThat(speed).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("이동 속도를 계산한다 (50km를 30분에 이동)")
    void calculateSpeed100KmPer30Minutes() {
        // given
        double distance = 50.0; // km
        long timeDiffSeconds = 1800; // 30분

        // when
        double speed = AnomalyDetector.calculateSpeed(distance, timeDiffSeconds);

        // then
        assertThat(speed).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("시간이 0이고 거리가 100km이면 무한대 속도를 반환한다")
    void calculateSpeedWhenTimeIsZeroWithDistance() {
        // given
        double distance = 100.0;
        long timeDiffSeconds = 0;

        // when
        double speed = AnomalyDetector.calculateSpeed(distance, timeDiffSeconds);

        // then
        assertThat(speed).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    @DisplayName("시간이 0이고 거리도 0이면 속도는 0이다")
    void calculateSpeedWhenTimeAndDistanceAreZero() {
        // given
        double distance = 0.0;
        long timeDiffSeconds = 0;

        // when
        double speed = AnomalyDetector.calculateSpeed(distance, timeDiffSeconds);

        // then
        assertThat(speed).isEqualTo(0.0);
    }

    @Test
    @DisplayName("시간이 음수이고 거리가 있으면 무한대 속도를 반환한다")
    void calculateSpeedWhenTimeIsNegativeWithDistance() {
        // given
        double distance = 100.0;
        long timeDiffSeconds = -100;

        // when
        double speed = AnomalyDetector.calculateSpeed(distance, timeDiffSeconds);

        // then
        assertThat(speed).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    @DisplayName("시간이 음수이고 거리가 0이면 속도는 0이다")
    void calculateSpeedWhenTimeIsNegativeWithoutDistance() {
        // given
        double distance = 0.0;
        long timeDiffSeconds = -100;

        // when
        double speed = AnomalyDetector.calculateSpeed(distance, timeDiffSeconds);

        // then
        assertThat(speed).isEqualTo(0.0);
    }

    @Test
    @DisplayName("정상 이동 (80km/h)은 레벨 0을 반환한다")
    void classifyNormalMovement() {
        // given
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 37.6665;
        double lng2 = 127.0780;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 10, 0); // 10분 후

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(0); // 정상
    }

    @Test
    @DisplayName("의심 이동 (150km/h 이상)은 레벨 1을 반환한다")
    void classifySuspiciousMovement() {
        // given - 서울에서 대전까지 약 140km를 30분에 이동 (280km/h)
        double seoulLat = 37.5665;
        double seoulLng = 126.9780;
        double daejeonLat = 36.3504;
        double daejeonLng = 127.3845;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 30, 0); // 30분 후

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(
                seoulLat, seoulLng, daejeonLat, daejeonLng, time1, time2);

        // then
        assertThat(level).isIn(1, 2); // 의심 또는 강한 의심
    }

    @Test
    @DisplayName("강한 의심 이동 (300km/h 이상)은 레벨 2를 반환한다")
    void classifyHighSuspicionMovement() {
        // given - 서울에서 부산까지 약 325km를 1시간에 이동 (325km/h)
        double seoulLat = 37.5665;
        double seoulLng = 126.9780;
        double busanLat = 35.1796;
        double busanLng = 129.0756;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 11, 0, 0); // 1시간 후

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(
                seoulLat, seoulLng, busanLat, busanLng, time1, time2);

        // then
        assertThat(level).isEqualTo(2); // 강한 의심
    }

    @Test
    @DisplayName("즉시 차단급 이동 (1000km/h 이상)은 레벨 3을 반환한다")
    void classifyExtremeMovement() {
        // given - 서울에서 부산까지 약 325km를 10분에 이동 (1950km/h)
        double seoulLat = 37.5665;
        double seoulLng = 126.9780;
        double busanLat = 35.1796;
        double busanLng = 129.0756;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 10, 0); // 10분 후

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(
                seoulLat, seoulLng, busanLat, busanLng, time1, time2);

        // then
        assertThat(level).isEqualTo(3); // 즉시 차단
    }

    @Test
    @DisplayName("동일 시간에 위치가 크게 변경되면 즉시 차단한다")
    void ignoreMovementWithinOneSecond() {
        // given
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 35.1796;
        double lng2 = 129.0756;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 0); // 동일 시간

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(3);
    }

    @Test
    @DisplayName("서버와 클라이언트 시간 차이가 5분이면 정상이다")
    void normalTimeDifference() {
        // given
        LocalDateTime serverTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime clientTime = LocalDateTime.of(2025, 1, 1, 10, 5, 0); // 5분 차이

        // when
        boolean isManipulation = AnomalyDetector.isTimeManipulation(serverTime, clientTime);

        // then
        assertThat(isManipulation).isFalse();
    }

    @Test
    @DisplayName("서버와 클라이언트 시간 차이가 15분이면 시간 조작 의심이다")
    void suspectTimeManipulation() {
        // given
        LocalDateTime serverTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime clientTime = LocalDateTime.of(2025, 1, 1, 10, 15, 0); // 15분 차이

        // when
        boolean isManipulation = AnomalyDetector.isTimeManipulation(serverTime, clientTime);

        // then
        assertThat(isManipulation).isTrue();
    }

    @Test
    @DisplayName("클라이언트 시간이 null이면 검증하지 않는다")
    void ignoreNullClientTime() {
        // given
        LocalDateTime serverTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);

        // when
        boolean isManipulation = AnomalyDetector.isTimeManipulation(serverTime, null);

        // then
        assertThat(isManipulation).isFalse();
    }

    @Test
    @DisplayName("유효한 GPS 좌표를 검증한다")
    void validateCoordinate() {
        // when & then
        assertThat(AnomalyDetector.isValidCoordinate(37.5665, 126.9780)).isTrue();
        assertThat(AnomalyDetector.isValidCoordinate(0.0, 0.0)).isTrue();
        assertThat(AnomalyDetector.isValidCoordinate(-90.0, -180.0)).isTrue();
        assertThat(AnomalyDetector.isValidCoordinate(90.0, 180.0)).isTrue();
    }

    @Test
    @DisplayName("범위를 벗어난 GPS 좌표는 무효하다")
    void invalidCoordinateOutOfRange() {
        // when & then
        assertThat(AnomalyDetector.isValidCoordinate(91.0, 0.0)).isFalse();
        assertThat(AnomalyDetector.isValidCoordinate(-91.0, 0.0)).isFalse();
        assertThat(AnomalyDetector.isValidCoordinate(0.0, 181.0)).isFalse();
        assertThat(AnomalyDetector.isValidCoordinate(0.0, -181.0)).isFalse();
    }

    @Test
    @DisplayName("null 좌표는 무효하다")
    void invalidNullCoordinate() {
        // when & then
        assertThat(AnomalyDetector.isValidCoordinate(null, 126.9780)).isFalse();
        assertThat(AnomalyDetector.isValidCoordinate(37.5665, null)).isFalse();
        assertThat(AnomalyDetector.isValidCoordinate(null, null)).isFalse();
    }

    @Test
    @DisplayName("동일 시간이지만 GPS 오차 범위 내(100m 미만)는 정상이다")
    void ignoreSameTimeWithinGpsError() {
        // given - 동일 시간, 50m 이동
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 37.5670; // 약 55m 이동
        double lng2 = 126.9785;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 0); // 동일 시간

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(0);
    }

    @Test
    @DisplayName("동일 시간에 1m 이상 이동하면 즉시 차단한다")
    void detectSameTimeWithMinimalMovement() {
        // given - 동일 시간, 1km 이동
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 37.5765; // 약 1.1km 이동
        double lng2 = 126.9780;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 0); // 동일 시간

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(3);
    }

    @Test
    @DisplayName("GPS 오차 범위 내(100m 미만)는 항상 정상이다")
    void ignoreMovementWithinGpsErrorRange() {
        // given - 동일 위치에서 50m 이동
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 37.5670; // 약 55m 이동
        double lng2 = 126.9785;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 0); // 동일 시간

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(0); // GPS 오차 범위
    }

    @Test
    @DisplayName("동일 시간에 200m 미만 이동은 GPS 재연결 오차로 간주한다")
    void ignoreSameTimeWithinReconnectionError() {
        // given - 동일 시간, 150m 이동
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 37.5678; // 약 144m 이동
        double lng2 = 126.9795;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 0); // 동일 시간

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(0); // GPS 재연결 오차 허용
    }

    @Test
    @DisplayName("동일 시간에 200m 이상 이동하면 즉시 차단한다")
    void detectSameTimeOver200m() {
        // given - 동일 시간, 250m 이동
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 37.5687; // 약 244m 이동
        double lng2 = 126.9805;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 0); // 동일 시간

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(3); // 즉시 차단
    }

    @Test
    @DisplayName("동일 시간에 서울-부산(325km)처럼 명백한 조작은 즉시 차단한다")
    void detectSameTimeExtremeDistance() {
        // given - 서울에서 부산으로 325km 이동, 시간은 동일
        double seoulLat = 37.5665;
        double seoulLng = 126.9780;
        double busanLat = 35.1796;
        double busanLng = 129.0756;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 0); // 동일 시간

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(
                seoulLat, seoulLng, busanLat, busanLng, time1, time2);

        // then
        assertThat(level).isEqualTo(3); // 즉시 차단
    }

    @Test
    @DisplayName("시간 역행(음수) + 100m 미만 이동은 정상으로 간주한다")
    void ignoreNegativeTimeWithinGpsError() {
        // given - 과거로 10초 + 50m 이동
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 37.5670; // 약 55m 이동
        double lng2 = 126.9785;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 10);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 0); // 10초 과거

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(0); // GPS 오차 범위
    }

    @Test
    @DisplayName("시간 역행(음수) + 100m 이상 이동은 즉시 차단한다")
    void detectNegativeTimeWithMovement() {
        // given - 과거로 10초 + 150m 이동
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 37.5678; // 약 144m 이동
        double lng2 = 126.9795;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 10);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 0); // 10초 과거

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(3); // 즉시 차단
    }

    @Test
    @DisplayName("1분 미만 + 200m 미만 이동은 정상으로 간주한다")
    void ignoreShortTimeShortDistance() {
        // given - 30초 + 150m 이동
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 37.5678; // 약 144m 이동
        double lng2 = 126.9795;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 30); // 30초 후

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(0); // 짧은 시간 짧은 거리 허용
    }

    @Test
    @DisplayName("1분 미만이지만 200m 이상 이동하면 속도 기반으로 판단한다")
    void classifyShortTimeButLongDistance() {
        // given - 30초 + 300m 이동 (36km/h)
        double lat1 = 37.5665;
        double lng1 = 126.9780;
        double lat2 = 37.5692; // 약 300m 이동
        double lng2 = 126.9815;
        LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 10, 0, 30); // 30초 후

        // when
        int level = AnomalyDetector.classifyMovementAnomaly(lat1, lng1, lat2, lng2, time1, time2);

        // then
        assertThat(level).isEqualTo(0); // 36km/h는 정상 속도
    }
}
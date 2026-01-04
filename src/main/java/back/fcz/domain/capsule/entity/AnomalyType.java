package back.fcz.domain.capsule.entity;

public enum AnomalyType {
    NONE,                   // 이상 없음
    IMPOSSIBLE_MOVEMENT,    // 물리적으로 불가능한 이동
    TIME_MANIPULATION,      // 시간 조작 의심
    RAPID_RETRY,           // 짧은 시간 내 반복 시도
    LOCATION_RETRY,        // 위치 조건 반복 실패
    SUSPICIOUS_PATTERN     // 기타 의심 패턴
}

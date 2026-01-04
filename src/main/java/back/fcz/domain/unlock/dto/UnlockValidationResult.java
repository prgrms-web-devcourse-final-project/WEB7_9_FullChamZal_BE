package back.fcz.domain.unlock.dto;

import back.fcz.domain.capsule.entity.AnomalyType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnlockValidationResult {

    // 조건 검증 결과
    private final boolean conditionMet;  // 시간/위치 조건 충족 여부

    // 이상 활동 감지 결과
    private final AnomalyType anomalyType;  // 감지된 이상 유형
    private final int suspicionScore;       // 이번 시도의 의심 점수

    // 편의 메서드들
    public boolean hasAnomaly() {
        return anomalyType != AnomalyType.NONE;
    }

    public boolean isSuccess() {
        return conditionMet && !hasAnomaly();
    }

    public boolean isFailure() {
        return !conditionMet || hasAnomaly();
    }

    // 정상 검증 성공 결과 생성
    public static UnlockValidationResult success() {
        return UnlockValidationResult.builder()
                .conditionMet(true)
                .anomalyType(AnomalyType.NONE)
                .suspicionScore(0)
                .build();
    }

    // 조건 미충족 결과 생성
    public static UnlockValidationResult conditionFailed() {
        return UnlockValidationResult.builder()
                .conditionMet(false)
                .anomalyType(AnomalyType.NONE)
                .suspicionScore(0)
                .build();
    }

    // 이상 감지 결과 생성
    public static UnlockValidationResult anomalyDetected(
            boolean conditionMet,
            AnomalyType anomalyType,
            int suspicionScore
    ) {
        return UnlockValidationResult.builder()
                .conditionMet(conditionMet)
                .anomalyType(anomalyType)
                .suspicionScore(suspicionScore)
                .build();
    }
}

package back.fcz.domain.sanction.constant;

import back.fcz.domain.capsule.entity.AnomalyType;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import back.fcz.domain.sanction.properties.SanctionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SanctionConstants  {

    private final SanctionProperties sanctionProperties;
    private final MemberRepository memberRepository;

    public static final String SYSTEM_ADMIN_USER_ID = "SYSTEM";
    public static final String AUTO_SANCTION_REASON_PREFIX = "자동 제재: ";

    // 캐싱된 시스템 관리자 ID (애플리케이션 시작 시 초기화)
    private volatile Long systemAdminIdCache;

    public Long getSystemAdminId() {
        if (systemAdminIdCache != null) {
            return systemAdminIdCache;
        }

        synchronized (this) {
            if (systemAdminIdCache == null) {
                systemAdminIdCache = memberRepository.findByUserId(SYSTEM_ADMIN_USER_ID)
                        .map(Member::getMemberId)
                        .orElseThrow(() ->
                                new IllegalStateException("SYSTEM 계정이 존재하지 않습니다."));
                log.info("시스템 관리자 ID Lazy 초기화 완료: {}", systemAdminIdCache);
            }
        }
        return systemAdminIdCache;
    }

    // 이상 유형별 의심 점수 반환
    public int getScoreByAnomaly(AnomalyType anomalyType) {
        if (sanctionProperties.getMonitoring() == null
                || sanctionProperties.getMonitoring().getAnomalyScores() == null) {
            log.warn("anomaly-scores 설정이 없습니다. 기본값 0을 반환합니다.");
            return 0;
        }

        Integer score = sanctionProperties.getMonitoring()
                .getAnomalyScores()
                .get(anomalyType);

        if (score == null) {
            log.warn("anomaly-scores에 {}에 대한 설정이 없습니다. 기본값 0을 반환합니다.", anomalyType);
            return 0;
        }

        return score;
    }

    // 자동 제재 사유 생성
    public static String buildAutoSanctionReason(String detail) {
        return AUTO_SANCTION_REASON_PREFIX + detail;
    }
}

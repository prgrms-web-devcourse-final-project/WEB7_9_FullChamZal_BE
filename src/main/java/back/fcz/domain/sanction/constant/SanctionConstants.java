package back.fcz.domain.sanction.constant;

import back.fcz.domain.capsule.entity.AnomalyType;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;

public class SanctionConstants  {

    public static final String SYSTEM_ADMIN_USER_ID = "SYSTEM";
    public static final String AUTO_SANCTION_REASON_PREFIX = "자동 제재: ";

    // 이상 유형별 의심 점수
    public static final int SCORE_IMPOSSIBLE_MOVEMENT = 50;
    public static final int SCORE_TIME_MANIPULATION = 30;
    public static final int SCORE_RAPID_RETRY = 20;
    public static final int SCORE_LOCATION_RETRY = 15;
    public static final int SCORE_SUSPICIOUS_PATTERN = 10;

    // 시스템 관리자 memberId 조회 (자동 제재 기록용)
    public static Long getSystemAdminId(MemberRepository memberRepository) {
        return memberRepository.findByUserId(SYSTEM_ADMIN_USER_ID)
                .map(Member::getMemberId)
                .orElseThrow(() -> new IllegalStateException(
                        "시스템 관리자 계정(SYSTEM)을 찾을 수 없습니다. 서버 초기화 실패"));
    }

    // 자동 제재 사유 생성
    public static String buildAutoSanctionReason(String detail) {
        return AUTO_SANCTION_REASON_PREFIX + detail;
    }

    // 자동 제재 여부 확인
    public static boolean isAutoSanctionReason(String reason) {
        return reason != null && reason.startsWith(AUTO_SANCTION_REASON_PREFIX);
    }

    // 시스템 관리자 여부 확인 (userId 기준)
    public static boolean isSystemAdminByUserId(String userId) {
        return SYSTEM_ADMIN_USER_ID.equals(userId);
    }

    // 시스템 관리자 여부 확인 (Member 엔티티 기준)
    public static boolean isSystemAdmin(Member member) {
        return member != null && SYSTEM_ADMIN_USER_ID.equals(member.getUserId());
    }

    // 이상 유형별 의심 점수 반환
    public static int getScoreByAnomaly(AnomalyType anomalyType) {
        return switch (anomalyType) {
            case IMPOSSIBLE_MOVEMENT -> SCORE_IMPOSSIBLE_MOVEMENT;
            case TIME_MANIPULATION -> SCORE_TIME_MANIPULATION;
            case RAPID_RETRY -> SCORE_RAPID_RETRY;
            case LOCATION_RETRY -> SCORE_LOCATION_RETRY;
            case SUSPICIOUS_PATTERN -> SCORE_SUSPICIOUS_PATTERN;
            case NONE -> 0;
        };
    }

    private SanctionConstants() {
        throw new AssertionError("상수 클래스는 인스턴스화할 수 없습니다.");
    }
}

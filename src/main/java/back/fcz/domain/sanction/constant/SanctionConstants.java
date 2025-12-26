package back.fcz.domain.sanction.constant;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Component;

@Component
public class SanctionConstants  {

    public static final String SYSTEM_ADMIN_USER_ID = "SYSTEM";
    public static final String AUTO_SANCTION_REASON_PREFIX = "자동 제재: ";

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

    private SanctionConstants() {
        throw new AssertionError("상수 클래스는 인스턴스화할 수 없습니다.");
    }
}

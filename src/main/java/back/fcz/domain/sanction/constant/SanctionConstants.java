package back.fcz.domain.sanction.constant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SanctionConstants  {

    public static final Long SYSTEM_ADMIN_ID = 0L;
    public static final String SYSTEM_ADMIN_USER_ID = "SYSTEM";
    public static final String AUTO_SANCTION_REASON_PREFIX = "자동 제재: ";

    // 시스템 관리자 memberId 조회 (자동 제재 기록용)
    public static Long getSystemAdminId() {
        return SYSTEM_ADMIN_ID;
    }

    // 자동 제재 사유 생성
    public static String buildAutoSanctionReason(String reason, int score) {
        return AUTO_SANCTION_REASON_PREFIX + reason + " (의심 점수: " + score + "점)";
    }

    // 자동 제재 사유인지 확인
    public static boolean isAutoSanctionReason(String reason) {
        return reason != null && reason.startsWith(AUTO_SANCTION_REASON_PREFIX);
    }

    // adminId가 시스템 자동 제재인지 확인
    public static boolean isSystemAdmin(Long adminId) {
        return SYSTEM_ADMIN_ID.equals(adminId);
    }
}

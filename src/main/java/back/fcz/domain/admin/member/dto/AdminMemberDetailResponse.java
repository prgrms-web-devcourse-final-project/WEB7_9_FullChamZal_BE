package back.fcz.domain.admin.member.dto;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class AdminMemberDetailResponse {

    // 기본 프로필 정보
    private final Long id;                 // member.memberId
    private final String userId;           // 로그인 ID
    private final String name;             // 실명
    private final String nickname;         // 닉네임
    private final MemberStatus status;     // ACTIVE / STOP / EXIT
    private final String phoneNumber;      // 암호화된 전화번호(우선 그대로, 나중에 마스킹 가능)

    private final LocalDateTime createdAt;             // 가입일
    private final LocalDateTime updatedAt;             // 마지막 수정일
    private final LocalDateTime lastNicknameChangedAt; // 닉네임 마지막 변경일

    // 통계 정보 (추후 실제 집계 로직 연결)
    private final long totalCapsuleCount;        // 작성한 캡슐 수
    private final long totalReportCount;         // 신고 누적 수
    private final long totalBookmarkCount;       // 즐겨찾기 수
    private final long totalBlockedCapsuleCount; // 블라인드/숨김 처리된 캡슐 수
    private final long storyTrackCount;          // 스토리 트랙 수

    // 최근 활동 정보 (추후 실제 데이터 연결)
    private final List<RecentCapsuleSummary> recentCapsules;
    private final List<RecentNotificationSummary> recentNotifications;
    private final List<PhoneVerificationLog> recentPhoneVerifications;

    public static AdminMemberDetailResponse basicOf(Member member) {
        return AdminMemberDetailResponse.builder()
                .id(member.getMemberId())
                .userId(member.getUserId())
                .name(member.getName())
                .nickname(member.getNickname())
                .status(member.getStatus())
                .phoneNumber(member.getPhoneNumber())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .lastNicknameChangedAt(member.getNicknameChangedAt())

                // 아직 통계/로그는 안 붙였으니 0 + emptyList 로 채워둠 (TODO)
                .totalCapsuleCount(0L)
                .totalReportCount(0L)
                .totalBookmarkCount(0L)
                .totalBlockedCapsuleCount(0L)
                .storyTrackCount(0L)

                .recentCapsules(Collections.emptyList())
                .recentNotifications(Collections.emptyList())
                .recentPhoneVerifications(Collections.emptyList())
                .build();
    }

    // ===== 내부 서브 DTO들 =====

    @Getter
    @Builder
    public static class RecentCapsuleSummary {
        private final Long id;
        private final String title;
        private final String status;        // 캡슐 상태 (문자열로)
        private final String visibility;    // PUBLIC / PRIVATE
        private final LocalDateTime createdAt;
        private final long openCount;
        private final long reportCount;
    }

    @Getter
    @Builder
    public static class RecentNotificationSummary {
        private final Long id;
        private final String type;          // 알림 타입 (예: CAPSULE_OPEN, CAPSULE_UNLOCK ...)
        private final String message;       // 알림 내용 요약
        private final LocalDateTime sentAt; // 발송 시각
        private final boolean read;         // 읽음 여부
    }

    @Getter
    @Builder
    public static class PhoneVerificationLog {
        private final Long id;
        private final String purpose;           // SIGNUP / CHANGE_PHONE / GUEST_VERIFY
        private final String status;            // PENDING / VERIFIED / EXPIRED
        private final Integer attemptCount;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiredAt;
        private final LocalDateTime verifiedAt;
    }
}

package back.fcz.domain.admin.member.dto;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 회원 목록 row 한 줄에 해당하는 응답 DTO
 */
@Getter
@Builder
public class AdminMemberSummaryResponse {

    private final Long id;                 // member.id
    private final String userId;           // member.userId
    private final String nickname;         // member.nickname
    private final MemberStatus status;     // ACTIVE / STOP / EXIT
    private final String phoneNumber;      // 마스킹된 전화번호 "010-****-1234"

    private final long reportCount;        // 신고 누적 횟수
    private final long blockedCapsuleCount;// 차단된(블라인드) 캡슐 수
    private final long capsuleCount;       // 전체 작성 캡슐 수

    private final LocalDateTime createdAt; // 가입일시

    public static AdminMemberSummaryResponse of(
            Member member,
            long reportCount,
            long blockedCapsuleCount,
            long capsuleCount
    ) {
        return AdminMemberSummaryResponse.builder()
                .id(member.getMemberId())
                .userId(member.getUserId())
                .nickname(member.getNickname())
                .status(member.getStatus())
                // 여기서는 일단 그대로 내려주고, 나중에 마스킹 로직 추가해도 됨
                .phoneNumber(member.getPhoneNumber())
                .reportCount(reportCount)
                .blockedCapsuleCount(blockedCapsuleCount)
                .capsuleCount(capsuleCount)
                .createdAt(member.getCreatedAt())
                .build();
    }
}

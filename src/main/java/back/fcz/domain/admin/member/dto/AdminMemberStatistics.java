package back.fcz.domain.admin.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 회원 통계 정보를 한 번의 쿼리로 조회하기 위한 DTO
 */
@Getter
@AllArgsConstructor
public class AdminMemberStatistics {
    private Long memberId;
    private Long totalCapsuleCount;      // 전체 캡슐 수
    private Long protectedCapsuleCount;  // 보호(블라인드) 캡슐 수
    private Long reportedCount;          // 신고당한 횟수

    public AdminMemberStatistics(Long memberId, Long totalCapsuleCount, Long protectedCapsuleCount) {
        this.memberId = memberId;
        this.totalCapsuleCount = totalCapsuleCount;
        this.protectedCapsuleCount = protectedCapsuleCount;
        this.reportedCount = 0L;
    }
}

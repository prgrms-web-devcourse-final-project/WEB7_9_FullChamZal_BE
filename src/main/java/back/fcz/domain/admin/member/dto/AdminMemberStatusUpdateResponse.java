package back.fcz.domain.admin.member.dto;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 1-3 회원 상태 변경 응답 DTO
 */
@Getter
@Builder
public class AdminMemberStatusUpdateResponse {

    private final Long id;                 // member.memberId
    private final String nickname;         // 닉네임
    private final MemberStatus status;     // 변경된 상태

    // 아직 DB 컬럼은 없지만, 프론트 표시용/향후 확장용으로 포함
    private final String reason;
    private final LocalDateTime sanctionUntil;

    public static AdminMemberStatusUpdateResponse of(
            Member member,
            String reason,
            LocalDateTime sanctionUntil
    ) {
        return AdminMemberStatusUpdateResponse.builder()
                .id(member.getMemberId())
                .nickname(member.getNickname())
                .status(member.getStatus())
                .reason(reason)
                .sanctionUntil(sanctionUntil)
                .build();
    }
}

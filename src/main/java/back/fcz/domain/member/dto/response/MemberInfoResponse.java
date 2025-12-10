package back.fcz.domain.member.dto.response;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;

import java.time.LocalDateTime;

/**
 * 회원 기본 정보 응답 DTO (전화번호 마스킹)
 * - 전화번호: 마스킹 처리된 형태 (예: 010-****-5678)
 */
public record MemberInfoResponse(
        Long memberId,
        String userId,
        String name,
        String nickname,
        String phoneNumber,  // 마스킹된 전화번호
        MemberStatus status,
        MemberRole role,
        LocalDateTime createdAt
) {
    /**
     * Entity와 마스킹된 전화번호로부터 DTO 생성
     */
    public static MemberInfoResponse of(Member member, String maskedPhone) {
        return new MemberInfoResponse(
                member.getMemberId(),
                member.getUserId(),
                member.getName(),
                member.getNickname(),
                maskedPhone,
                member.getStatus(),
                member.getRole(),
                member.getCreatedAt()
        );
    }
}
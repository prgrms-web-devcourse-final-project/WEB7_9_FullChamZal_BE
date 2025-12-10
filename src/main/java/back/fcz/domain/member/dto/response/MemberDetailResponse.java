package back.fcz.domain.member.dto.response;

import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.entity.OAuthProvider;

import java.time.LocalDateTime;

/**
 * 회원 상세 정보 응답 DTO (비밀번호 확인 후 전체 정보 조회)
 * - 전화번호: 복호화된 원본
 */
public record MemberDetailResponse(
        Long memberId,
        String userId,
        String name,
        String nickname,
        String phoneNumber,  // 복호화된 전화번호
        MemberStatus status,
        MemberRole role,
        OAuthProvider oauthProvider,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Entity와 복호화된 전화번호로부터 DTO 생성
     */
    public static MemberDetailResponse of(Member member, String decryptedPhone) {
        return new MemberDetailResponse(
                member.getMemberId(),
                member.getUserId(),
                member.getName(),
                member.getNickname(),
                decryptedPhone,
                member.getStatus(),
                member.getRole(),
                member.getOauthProvider(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
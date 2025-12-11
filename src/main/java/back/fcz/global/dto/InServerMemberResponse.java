package back.fcz.global.dto;

import back.fcz.domain.member.entity.MemberRole;
import back.fcz.domain.member.entity.Member;

// 다른 도메인에서 회원 정보 조회 시 사용하는 DTO
public record InServerMemberResponse(
        Long memberId,
        String userId,
        String name,
        String nickname,
        String phoneNumber,  // 암호화된 상태
        String phoneHash,    // 해시
        MemberRole role
) {

    // 전화번호 암호화 버전
    public static InServerMemberResponse from(Member member) {
        return new InServerMemberResponse(
                member.getMemberId(),
                member.getUserId(),
                member.getName(),
                member.getNickname(),
                member.getPhoneNumber(),
                member.getPhoneHash(),
                member.getRole()
        );
    }

    // 전화번호 복호화 버전
    public InServerMemberResponse withDecryptedPhone(String decryptedPhone) {
        return new InServerMemberResponse(
                this.memberId,
                this.userId,
                this.name,
                this.nickname,
                decryptedPhone,
                this.phoneHash,
                this.role
        );
    }
}
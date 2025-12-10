package back.fcz.domain.member.dto.response;

/**
 * 회원가입 응답 DTO
 */
public record MemberSignupResponse(
        Long memberId,
        String userId
) {
    public static MemberSignupResponse of(Long memberId, String userId) {
        return new MemberSignupResponse(memberId, userId);
    }
}
package back.fcz.domain.capsule.DTO.response;

public record CapsuleReadResponse(
        Long capsuleId,
        int isProtected,
        boolean existedPassword  // 패스워드 존재 유무(true면 존재하고 false면 존재하지 않는 것)
) {
    public static CapsuleReadResponse from(Long capsuleId, int isProtected, boolean existedPassword) {
        return new CapsuleReadResponse(capsuleId, isProtected, existedPassword);
    }
}

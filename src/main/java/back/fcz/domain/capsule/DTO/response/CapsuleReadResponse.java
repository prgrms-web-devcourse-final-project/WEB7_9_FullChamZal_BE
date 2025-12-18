package back.fcz.domain.capsule.DTO.response;

public record CapsuleReadResponse(
        boolean existedPassword  // 패스워드 존재 유무(true면 존재하고 false면 존재하지 않는 것)
) {
    public static CapsuleReadResponse from(boolean existedPassword) {
        return new CapsuleReadResponse(existedPassword);
    }
}

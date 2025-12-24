package back.fcz.domain.capsule.DTO.response;

public record CapsuleLikeResponse(
        int capsuleLikeCount,
        String message
) {
    public static CapsuleLikeResponse from(int capsuleLikeCount, String message) {
        return new CapsuleLikeResponse(
                capsuleLikeCount,
                message
        );
    }
}

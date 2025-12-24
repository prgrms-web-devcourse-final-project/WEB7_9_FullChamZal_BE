package back.fcz.domain.capsule.DTO.response;

public record CapsuleLikeReadResponse(
        int capsuleLikeCount,
        String message,
        boolean isLiked
) {
    public static CapsuleLikeReadResponse from(int capsuleLikeCount, String message, boolean isLiked) {
        return new CapsuleLikeReadResponse(
                capsuleLikeCount,
                message,
                isLiked
        );
    }
}

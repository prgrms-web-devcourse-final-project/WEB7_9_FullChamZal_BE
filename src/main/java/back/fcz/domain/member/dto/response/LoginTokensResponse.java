package back.fcz.domain.member.dto.response;

public record LoginTokensResponse(
        String accessToken,
        String refreshToken
) {
}
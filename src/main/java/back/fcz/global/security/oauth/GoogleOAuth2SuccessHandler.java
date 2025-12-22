package back.fcz.global.security.oauth;

import back.fcz.domain.member.entity.Member;
import back.fcz.global.security.jwt.CookieProperties;
import back.fcz.global.security.jwt.JwtProperties;
import back.fcz.global.security.jwt.JwtProvider;
import back.fcz.global.security.jwt.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final RefreshTokenService refreshTokenService;
    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;
    private final JwtProvider jwtProvider;

    @Value("${cors.allowed-origins}")
    private String frontendDomain;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) throws IOException {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Member member = principal.member();

        boolean isNewMember = (member.getNickname() == null);

        // access, refresh 토큰 생성
        String accessToken = jwtProvider.generateMemberAccessToken(member.getMemberId(), member.getRole().name());
        String refreshToken = jwtProvider.generateMemberRefreshToken(member.getMemberId(), member.getRole().name());

        refreshTokenService.saveMemberRefreshToken(
                member.getMemberId(),
                refreshToken,
                jwtProperties.getRefreshToken().getExpiration() / 1000
        );

        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", accessToken)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .domain(cookieProperties.getDomain())
                .path("/")
                .maxAge(jwtProperties.getAccessToken().getExpiration() / 1000)
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .domain(cookieProperties.getDomain())
                .path("/")
                .maxAge(jwtProperties.getRefreshToken().getExpiration() / 1000)
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 프론트엔드로 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(frontendDomain + "/dashboard")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

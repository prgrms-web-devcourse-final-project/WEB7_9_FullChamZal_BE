package back.fcz.global.security.oauth;

import back.fcz.domain.backup.repository.BackupRepository;
import back.fcz.domain.backup.service.BackupService;
import back.fcz.domain.backup.service.GoogleTokenRedisService;
import back.fcz.domain.member.entity.Member;
import back.fcz.global.crypto.PhoneCrypto;
import back.fcz.global.security.jwt.CookieProperties;
import back.fcz.global.security.jwt.JwtProperties;
import back.fcz.global.security.jwt.JwtProvider;
import back.fcz.global.security.jwt.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;

/**
 * 구글 OAuth 2.0 로그인 성공 후처리를 담당하는 핸들러
 *  - 서비스 로그인을 위한 JWT 발급 및 쿠키 설정
 *  - 구글 드라이브 API 접근을 위한 권한 토큰(access, refresh) 추출 및 DB 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final RefreshTokenService refreshTokenService;
    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;
    private final JwtProvider jwtProvider;

    private final BackupRepository backupRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final GoogleTokenRedisService googleTokenRedisService;
    private final BackupService backupService;
    private final PhoneCrypto phoneCrypto;

    @Value("${cors.allowed-origins}")
    private String frontendDomain;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) throws IOException {

        // 인증된 사용자 정보 추출
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Member member = principal.member();

        // JWT 토큰(access, refresh) 토큰 생성
        String accessToken = jwtProvider.generateMemberAccessToken(member.getMemberId(), member.getRole().name());
        String refreshToken = jwtProvider.generateMemberRefreshToken(member.getMemberId(), member.getRole().name());

        refreshTokenService.saveMemberRefreshToken(
                member.getMemberId(),
                refreshToken,
                jwtProperties.getRefreshToken().getExpiration() / 1000
        );

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // 토큰을 쿠키 설정
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

        // 구글 드라이브 접근 권한 토큰 추출
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        // 구글 드라이브 토큰 저장 또는 갱신
        saveGoogleToken(member.getMemberId(), client);

        // 로그인 처리가 완료된 후, 프론트엔드 대시보드로 이동
        String targetUrl = UriComponentsBuilder.fromUriString(frontendDomain + "/dashboard")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * 구글 드라이브 토큰을 저장 또는 업데이트
     *  - access 토큰은 Redis로 관리
     *  - refresh 토큰은 암호화하여 DB에 저장
     *
     * @param memberId 회원 ID
     * @param client 구글로부터 발급받은 인증 정보
     */
    private void saveGoogleToken(Long memberId, OAuth2AuthorizedClient client) {
        String googleAccessToken = client.getAccessToken().getTokenValue();
        log.info("Google Access Token present: {}", googleAccessToken != null);

        // 구글 보안 정책상 '최초 동의' 시에만 refresh 토큰 제공됨 (이후 로그인 시, null일 수 있음)
        String googleRefreshToken = (client.getRefreshToken() != null)
                ? client.getRefreshToken().getTokenValue() : null;
        log.info("Google Refresh Token present: {}", client.getRefreshToken() != null);

        // 구글 access 토큰의 만료 시간 계산 (기본값 1시간)
        Instant expiresAt = client.getAccessToken().getExpiresAt();
        long expiresIn = (expiresAt != null)
                ? expiresAt.getEpochSecond() - Instant.now().getEpochSecond()
                : 3600L;

        // access, refresh 토큰 저장
        backupService.persistGoogleToken(memberId, googleAccessToken, googleRefreshToken, expiresIn);
    }
}

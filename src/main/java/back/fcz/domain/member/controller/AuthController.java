package back.fcz.domain.member.controller;

import back.fcz.domain.member.dto.request.MemberLoginRequest;
import back.fcz.domain.member.dto.request.MemberSignupRequest;
import back.fcz.domain.member.dto.response.LoginTokensResponse;
import back.fcz.domain.member.dto.response.MemberSignupResponse;
import back.fcz.domain.member.service.AuthService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import back.fcz.global.security.jwt.CookieProperties;
import back.fcz.global.security.jwt.JwtProperties;
import back.fcz.global.security.jwt.JwtProvider;
import back.fcz.global.security.jwt.service.RefreshTokenService;
import back.fcz.global.security.jwt.service.TokenBlacklistService;
import back.fcz.global.security.jwt.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(
        name = "인증 API",
        description = "회원가입, 로그인, 로그아웃, 토큰 재발급 등 인증 관련 API"
)
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;
    private final CookieUtil cookieUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;

    @Operation(summary = "회원가입", description = "회원가입 API입니다.")
    @ApiErrorCodeExample({
            ErrorCode.DUPLICATE_USER_ID,
            ErrorCode.DUPLICATE_NICKNAME,
            ErrorCode.DUPLICATE_PHONENUM,
            ErrorCode.PHONE_NOT_VERIFIED
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberSignupResponse>> signup(
            @Valid @RequestBody MemberSignupRequest request
    ) {
        MemberSignupResponse response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "로그인", description = "로그인 API로, 액세스 토큰과 리프레시 토큰을 쿠키로 반환합니다.")
    @ApiErrorCodeExample({
            ErrorCode.INVALID_USER_ID,
            ErrorCode.INVALID_PASSWORD
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @RequestBody MemberLoginRequest request,
            HttpServletResponse response
    ) {
        LoginTokensResponse tokens = authService.login(request);

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", tokens.refreshToken())
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .domain(cookieProperties.getDomain())
                .path("/")
                .maxAge(jwtProperties.getRefreshToken().getExpiration() / 1000)
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", tokens.accessToken())
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .domain(cookieProperties.getDomain())
                .path("/")
                .maxAge(jwtProperties.getAccessToken().getExpiration() / 1000)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());

        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "로그아웃", description = "로그아웃 API로, 호출 시 모든 쿠키를 삭제합니다." + " 토큰이 없거나 유효하지 않은 경우에도 쿠키 삭제 후 성공 응답을 반환합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            String accessToken = CookieUtil.getCookieValue(request, CookieUtil.ACCESS_TOKEN_COOKIE)
                    .orElse(null);

            if (accessToken != null) {
                tokenBlacklistService.addToBlacklist(accessToken);

                Long memberId = jwtProvider.extractMemberId(accessToken);
                refreshTokenService.deleteMemberRefreshToken(memberId);

                log.info("로그아웃 성공 - memberId: {}", memberId);
            } else {
                log.info("로그아웃 시도했으나 토큰 없음 (이미 로그아웃 상태)");
            }

        } catch (BusinessException e) {
            log.warn("로그아웃 처리 중 토큰 검증 실패: errorCode={}, message={}",
                    e.getErrorCode(), e.getMessage());
        }

        CookieUtil.deleteAllTokenCookies(response, cookieProperties.isSecure(), cookieProperties.getSameSite(), cookieProperties.getDomain());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token을 이용하여 새로운 Access Token을 발급합니다. " +
                    " Refresh Token은 쿠키에서 자동으로 추출됩니다."
    )
    @ApiErrorCodeExample({
            ErrorCode.TOKEN_NOT_FOUND,
            ErrorCode.TOKEN_INVALID,
            ErrorCode.TOKEN_EXPIRED
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            String refreshToken = CookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_NOT_FOUND));

            String newAccessToken = refreshTokenService.refreshAccessToken(refreshToken);

            ResponseCookie accessCookie = ResponseCookie.from(CookieUtil.ACCESS_TOKEN_COOKIE, newAccessToken)
                    .httpOnly(true)
                    .secure(cookieProperties.isSecure())
                    .sameSite(cookieProperties.getSameSite())
                    .domain(cookieProperties.getDomain())
                    .path("/")
                    .maxAge(jwtProperties.getAccessToken().getExpiration() / 1000)
                    .build();

            response.addHeader("Set-Cookie", accessCookie.toString());

            log.info("Access Token 재발급 성공");
            return ResponseEntity.ok(ApiResponse.success());
        } catch (BusinessException e) {
            log.warn("토큰 재발급 실패: errorCode={}, message={}", e.getErrorCode(), e.getMessage());
            throw e;
        }
    }

    @Operation(
            summary = "Google OAuth 2.0 로그인",
            description = """
        Google 계정을 이용한 소셜 로그인입니다.

        ---
        ### 동작 방식
        1. 프론트엔드 서버에서 해당 URL로 **브라우저 이동(GET)** 을 수행합니다.
        2. 백엔드 서버는 Google OAuth 인증 페이지로 **302 Redirect** 합니다.
        3. 사용자가 Google 계정으로 로그인합니다.
        4. Google 로그인 성공 후, 백엔드 서버의 OAuth2 콜백 엔드포인트로 리다이렉트됩니다.
        5. 백엔드 서버에서 다음 작업을 수행합니다:
           - 신규 회원: 회원 가입 처리 (DB에 이름, 이메일, 고유 ID 저장)
           - JWT Access Token / Refresh Token 생성
           - JWT를 Cookie 로 설정
        6. 백엔드 서버에서 지정한 **redirect URL (/dashboard)** 로 페이지가 이동합니다.
        7. 신규 회원은 구글 로그인 완료 후, 닉네임 설정과 전화번호 인증을 수행해야합니다.

        ---
        ### 쿠키 전달 방식
        - 로그인 성공 시, 백엔드 서버가 아래 쿠키를 HTTP Response Header(Set-Cookie) 로 내려줍니다.
          - Access Token Cookie
          - Refresh Token Cookie

        ---
        ### 주의 사항
        - fetch / axios 등의 API Client로 호출하면 정상 동작하지 않습니다.
        - **브라우저 페이지 이동 방식**으로 호출해야 합니다. 예) '<a href>', 'window.location.href'
        - 이 API는 JSON 응답을 반환하지 않습니다.
        """
    )
    @GetMapping("/oauth2/authorization/google")
    public void googleOAuthLoginDocs() {
        // Swagger 문서화를 위한 더미 엔드포인트
        // 실제 구글 OAuth 2.0 인증 흐름은 Spring Security가 Filter 단계에서 처리
    }
}

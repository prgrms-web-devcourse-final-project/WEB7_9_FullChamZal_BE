package back.fcz.domain.member.controller;

import back.fcz.domain.member.dto.request.MemberLoginRequest;
import back.fcz.domain.member.dto.request.MemberSignupRequest;
import back.fcz.domain.member.dto.response.LoginTokensResponse;
import back.fcz.domain.member.dto.response.MemberSignupResponse;
import back.fcz.domain.member.service.AuthService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import back.fcz.global.security.jwt.CookieProperties;
import back.fcz.global.security.jwt.JwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(summary = "회원가입", description = "회원가입 API입니다.")
    @ApiErrorCodeExample({
            ErrorCode.DUPLICATE_USER_ID,
            ErrorCode.DUPLICATE_NICKNAME,
            ErrorCode.DUPLICATE_PHONENUM
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
                .path("/")
                .maxAge(jwtProperties.getRefreshToken().getExpiration() / 1000)
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());

        ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", tokens.accessToken())
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(jwtProperties.getAccessToken().getExpiration() / 1000)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());

        return ResponseEntity.ok(ApiResponse.success());

    }
}

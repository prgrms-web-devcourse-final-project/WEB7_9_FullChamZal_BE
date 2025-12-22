package back.fcz.domain.member.controller;

import back.fcz.domain.member.dto.request.OAuthSignupRequest;
import back.fcz.domain.member.dto.response.MemberSignupResponse;
import back.fcz.domain.member.service.OAuthService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "OAuth API",
        description = "소셜 로그인 관련 API"
)
public class OAuthController {
    private final OAuthService oAuthService;

    @Operation(
            summary = "Google OAuth 2.0 로그인",
            description = """
        Google 계정을 이용한 소셜 로그인입니다.
        ---
        ### 동작 방식
        1. 프론트엔드 서버에서 해당 URL로 **브라우저 이동(GET)** 을 수행합니다.
        2. 백엔드 서버는 Google OAuth 인증 페이지로 **302 Redirect** 합니다.
        3. 사용자의 Google 계정으로 로그인합니다.
        4. 로그인 성공 후, 백엔드 서버의 OAuth2 콜백 엔드포인트로 리다이렉트됩니다.
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
        - 이 API는 JSON 응답을 반환하지 않습니다.
        - fetch / axios 등의 API Client로 호출하면 동작하지 않습니다.
        - **브라우저 페이지 이동 방식**으로 호출해야 합니다. 예) `<a> 태그`, `window.location.href`
        """
    )
    @GetMapping("/oauth2/authorization/google")
    public void googleOAuthLoginDocs() {
        // Swagger 문서화를 위한 더미 엔드포인트
        // 실제 구글 OAuth 2.0 인증 흐름은 Spring Security가 Filter 단계에서 처리
    }

    @Operation(
            summary = "Google OAuth 2.0 회원가입",
            description = "Google 소셜 로그인을 사용하는 회원의 경우, 첫 로그인 시 닉네임 설정과 전화번호 인증을 진행해야합니다. " +
                    "해당 API를 통해, 닉네임 설정과 전화번호 인증을 실시합니다."
    )
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.DUPLICATE_NICKNAME,
            ErrorCode.DUPLICATE_PHONENUM,
            ErrorCode.WITHDRAWN_PHONE_NUMBER
    })
    @PostMapping("/api/v1/oauth/signup")
    public ResponseEntity<ApiResponse<MemberSignupResponse>> oauthSignup(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody OAuthSignupRequest request
    ) {
        MemberSignupResponse response = oAuthService.oauthSignup(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

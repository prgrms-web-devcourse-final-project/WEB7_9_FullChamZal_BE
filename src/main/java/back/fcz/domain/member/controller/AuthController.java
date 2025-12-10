package back.fcz.domain.member.controller;

import back.fcz.domain.member.dto.request.MemberSignupRequest;
import back.fcz.domain.member.dto.response.MemberSignupResponse;
import back.fcz.domain.member.service.AuthService;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberSignupResponse>> signup(
            @Valid @RequestBody MemberSignupRequest request
    ) {
        MemberSignupResponse response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

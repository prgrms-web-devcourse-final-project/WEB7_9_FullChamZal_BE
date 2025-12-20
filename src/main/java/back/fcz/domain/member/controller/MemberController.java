package back.fcz.domain.member.controller;

import back.fcz.domain.member.dto.request.MemberUpdateRequest;
import back.fcz.domain.member.dto.request.PasswordVerifyRequest;
import back.fcz.domain.member.dto.response.MemberDetailResponse;
import back.fcz.domain.member.dto.response.MemberInfoResponse;
import back.fcz.domain.member.dto.response.MemberUpdateResponse;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.member.service.MemberService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.dto.InServerMemberResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import back.fcz.global.security.jwt.CookieProperties;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@Tag(
        name = "회원 API",
        description = "회원 정보 조회/수정, 닉네임 변경 등 회원 관리 API"
)
public class MemberController {

    private final MemberService memberService;
    private final CurrentUserContext currentUserContext;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final CookieProperties cookieProperties;

    @Operation(summary = "비밀번호 검증", description = "현재 로그인한 회원의 비밀번호를 검증합니다. 회원 정보 수정, 탈퇴 등 민감한 작업 전에 사용합니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_EXPIRED,
            ErrorCode.TOKEN_INVALID,
            ErrorCode.INVALID_PASSWORD,
    })
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(
            @Valid @RequestBody PasswordVerifyRequest request
            ) {
        InServerMemberResponse user = currentUserContext.getCurrentUser();
        memberService.verifyPassword(user, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "회원 정보", description = "회원 정보를 반환하는 API입니다. (전화번호는 마스킹 처리)")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_EXPIRED,
            ErrorCode.TOKEN_INVALID
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getMe() {
        InServerMemberResponse user = currentUserContext.getCurrentUser();
        MemberInfoResponse response = memberService.getMe(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "회원 정보", description = "회원 정보 원본을 반환하는 API입니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_EXPIRED,
            ErrorCode.TOKEN_INVALID
    })
    @GetMapping("/me/detail")
    public ResponseEntity<ApiResponse<MemberDetailResponse>> getDetailMe() {
        InServerMemberResponse user = currentUserContext.getCurrentUser();
        MemberDetailResponse response = memberService.getDetailMe(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @Operation(
            summary = "회원 정보 수정",
            description = "현재 로그인한 회원의 정보를 수정합니다. " +
                    "변경하고자 하는 *항목*만 요청에 포함하면 됩니다. " +
                    "비밀번호 변경 시 현재 비밀번호가 필수입니다."
    )
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.INVALID_PASSWORD,
            ErrorCode.DUPLICATE_NICKNAME,
            ErrorCode.NICKNAME_CHANGE_TOO_SOON,
            ErrorCode.DUPLICATE_PHONENUM,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.TOKEN_EXPIRED,
            ErrorCode.TOKEN_INVALID,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.PHONE_NOT_VERIFIED
    })
    public ResponseEntity<ApiResponse<MemberUpdateResponse>> updateMe(
            @Valid @RequestBody MemberUpdateRequest request
    ) {
        InServerMemberResponse user = currentUserContext.getCurrentUser();
        MemberUpdateResponse response = memberService.updateMember(user, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 회원을 탈퇴 처리합니다. " +
                    "탈퇴 시 모든 토큰이 무효화되며, 30일 후 개인정보가 익명화됩니다. " +
                    "탈퇴 일자부터 30일 동안은 동일한 아이디, 전화번호로 회원가입이 불가합니다."
    )
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_EXPIRED,
            ErrorCode.TOKEN_INVALID
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        InServerMemberResponse user = currentUserContext.getCurrentUser();
        memberService.delete(user);

        try {
            String accessToken = CookieUtil.getCookieValue(request, CookieUtil.ACCESS_TOKEN_COOKIE)
                    .orElse(null);

            if(accessToken != null) {
                tokenBlacklistService.addToBlacklist(accessToken);
                refreshTokenService.deleteMemberRefreshToken(user.memberId());

                log.info("회원 탈퇴 및 토큰 무효화 완료 - memberId: {}", user.memberId());
            } else {
                log.warn("회원 탈퇴 시 Access Token이 없음 - memberId: {}", user.memberId());
            }
        } catch (BusinessException e) {
            log.error("회원 탈퇴 중 토큰 무효화 실패 - memberId: {}, errorCode: {}",
                    user.memberId(), e.getErrorCode());
        }

        CookieUtil.deleteAllTokenCookies(response, cookieProperties.isSecure(), cookieProperties.getSameSite(), cookieProperties.getDomain());

        return ResponseEntity.ok(ApiResponse.success());
    }
}

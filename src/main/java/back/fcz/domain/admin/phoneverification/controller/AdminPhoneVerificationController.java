package back.fcz.domain.admin.phoneverification.controller;

import back.fcz.domain.admin.phoneverification.dto.AdminPhoneVerificationDetailResponse;
import back.fcz.domain.admin.phoneverification.dto.AdminPhoneVerificationSearchRequest;
import back.fcz.domain.admin.phoneverification.dto.AdminPhoneVerificationSummaryResponse;
import back.fcz.domain.admin.phoneverification.service.AdminPhoneVerificationService;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/phone-verifications")
@Tag(name = "Admin-PhoneVerification", description = "관리자 - 전화번호 인증 조회 API")
public class AdminPhoneVerificationController {

    private final AdminPhoneVerificationService adminPhoneVerificationService;

    /**
     * 4-1. 전화번호 인증 로그 목록 조회
     */
    @GetMapping
    @Operation(summary = "전화번호 인증 로그 목록", description = "관리자가 전화번호 인증 로그를 페이지네이션/필터로 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<AdminPhoneVerificationSummaryResponse>>> getPhoneVerifications(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AdminPhoneVerificationSearchRequest cond = AdminPhoneVerificationSearchRequest.of(
                page, size, purpose, status, from, to
        );

        PageResponse<AdminPhoneVerificationSummaryResponse> result =
                adminPhoneVerificationService.getPhoneVerifications(cond);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 4-2. 전화번호 인증 로그 상세 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "전화번호 인증 로그 상세", description = "관리자가 특정 전화번호 인증 로그를 상세 조회합니다.")
    @ApiErrorCodeExample({ErrorCode.ADMIN_PHONE_VERIFICATION_NOT_FOUND})
    public ResponseEntity<ApiResponse<AdminPhoneVerificationDetailResponse>> getPhoneVerificationDetail(
            @PathVariable Long id
    ) {
        AdminPhoneVerificationDetailResponse detail =
                adminPhoneVerificationService.getPhoneVerificationDetail(id);

        return ResponseEntity.ok(ApiResponse.success(detail));
    }
}

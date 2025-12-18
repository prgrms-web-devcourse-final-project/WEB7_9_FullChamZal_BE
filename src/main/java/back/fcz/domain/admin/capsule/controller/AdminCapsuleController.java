package back.fcz.domain.admin.capsule.controller;

import back.fcz.domain.admin.capsule.dto.AdminCapsuleDeleteRequest;
import back.fcz.domain.admin.capsule.dto.AdminCapsuleDetailResponse;
import back.fcz.domain.admin.capsule.dto.AdminCapsuleSummaryResponse;
import back.fcz.domain.admin.capsule.service.AdminCapsuleService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/capsules")
@Tag(name = "Admin-Capsule", description = "관리자 - 캡슐 관리 API")
public class AdminCapsuleController {

    private final AdminCapsuleService adminCapsuleService;

    /**
     * 캡슐 목록 조회 (필터/검색)
     *
     * GET /api/v1/admin/capsules?page=0&size=20&visibility=PUBLIC&isDeleted=0&isProtected=1&keyword=abc
     */
    @GetMapping
    @Operation(summary = "캡슐 목록 조회", description = "관리자가 캡슐 리스트를 페이지네이션/필터/키워드 검색으로 조회합니다.")
    @ApiErrorCodeExample({ErrorCode.UNAUTHORIZED, ErrorCode.ACCESS_DENIED})
    public ResponseEntity<ApiResponse<PageResponse<AdminCapsuleSummaryResponse>>> getCapsules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) Integer isDeleted,     // 0/1/2
            @RequestParam(required = false) Integer isProtected,   // 0/1
            @RequestParam(required = false) String keyword
    ) {
        PageResponse<AdminCapsuleSummaryResponse> response =
                adminCapsuleService.getCapsules(page, size, visibility, isDeleted, isProtected, keyword);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 캡슐 상세 조회
     */
    @GetMapping("/{capsuleId}")
    @Operation(summary = "캡슐 상세 조회", description = "관리자가 특정 캡슐의 상세 정보를 조회합니다.")
    @ApiErrorCodeExample({ErrorCode.UNAUTHORIZED, ErrorCode.ACCESS_DENIED, ErrorCode.ADMIN_CAPSULE_NOT_FOUND})
    public ResponseEntity<ApiResponse<AdminCapsuleDetailResponse>> getCapsuleDetail(
            @PathVariable Long capsuleId
    ) {
        AdminCapsuleDetailResponse response = adminCapsuleService.getCapsuleDetail(capsuleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 캡슐 삭제/복구
     */
    @PatchMapping("/{capsuleId}/deleted")
    @Operation(summary = "캡슐 삭제/복구", description = "관리자가 캡슐을 삭제 처리하거나 삭제를 복구합니다.")
    @ApiErrorCodeExample({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.ACCESS_DENIED,
            ErrorCode.ADMIN_CAPSULE_NOT_FOUND,
            ErrorCode.ADMIN_INVALID_CAPSULE_STATUS_CHANGE
    })
    public ResponseEntity<ApiResponse<AdminCapsuleDetailResponse>> updateCapsuleDeleted(
            @PathVariable Long capsuleId,
            @RequestBody @Valid AdminCapsuleDeleteRequest request
    ) {
        AdminCapsuleDetailResponse response =
                adminCapsuleService.updateCapsuleDeleted(capsuleId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

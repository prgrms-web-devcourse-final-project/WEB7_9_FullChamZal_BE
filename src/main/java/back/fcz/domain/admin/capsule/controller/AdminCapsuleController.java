package back.fcz.domain.admin.capsule.controller;

import back.fcz.domain.admin.capsule.dto.AdminCapsuleDeleteRequest;
import back.fcz.domain.admin.capsule.dto.AdminCapsuleDetailResponse;
import back.fcz.domain.admin.capsule.dto.AdminCapsuleSummaryResponse;
import back.fcz.domain.admin.capsule.service.AdminCapsuleService;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/capsules")
public class AdminCapsuleController {

    private final AdminCapsuleService adminCapsuleService;

    /**
     * 2-1. 캡슐 목록 조회
     *
     * GET /api/v1/admin/capsules?page=0&size=20&visibility=PUBLIC
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminCapsuleSummaryResponse>>> getCapsules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String visibility
    ) {
        PageResponse<AdminCapsuleSummaryResponse> response =
                adminCapsuleService.getCapsules(page, size, visibility);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 2-2. 캡슐 상세 조회
     *
     * GET /api/v1/admin/capsules/{capsuleId}
     */
    @GetMapping("/{capsuleId}")
    public ResponseEntity<ApiResponse<AdminCapsuleDetailResponse>> getCapsuleDetail(
            @PathVariable Long capsuleId
    ) {
        AdminCapsuleDetailResponse response = adminCapsuleService.getCapsuleDetail(capsuleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 2-3. 캡슐 삭제/복구
     *
     * PATCH /api/v1/admin/capsules/{capsuleId}/deleted
     *
     * body:
     * {
     *   "deleted": true,
     *   "reason": "부적절한 표현 포함"
     * }
     */
    @PatchMapping("/{capsuleId}/deleted")
    public ResponseEntity<ApiResponse<AdminCapsuleDetailResponse>> updateCapsuleDeleted(
            @PathVariable Long capsuleId,
            @RequestBody @Valid AdminCapsuleDeleteRequest request
    ) {
        AdminCapsuleDetailResponse response =
                adminCapsuleService.updateCapsuleDeleted(capsuleId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

package back.fcz.domain.admin.sanction.controller;

import back.fcz.domain.admin.sanction.dto.AdminSanctionSummaryResponse;
import back.fcz.domain.admin.sanction.service.AdminSanctionService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/members")
@Tag(name = "Admin-Sanction", description = "관리자 - 제재 관리 API")
public class AdminSanctionController {

    private final AdminSanctionService adminSanctionService;

    /**
     * 제재 이력 조회(페이징)
     * GET /api/v1/admin/members/{memberId}/sanctions?page=0&size=20
     */
    @GetMapping("/{memberId}/sanctions")
    @Operation(summary = "회원 제재 이력 조회", description = "관리자가 특정 회원의 제재 이력을 페이지네이션으로 조회합니다.")
    @ApiErrorCodeExample({ErrorCode.UNAUTHORIZED, ErrorCode.ACCESS_DENIED, ErrorCode.ADMIN_MEMBER_NOT_FOUND})
    public ResponseEntity<ApiResponse<PageResponse<AdminSanctionSummaryResponse>>> getMemberSanctions(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<AdminSanctionSummaryResponse> result =
                adminSanctionService.getMemberSanctions(memberId, page, size);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

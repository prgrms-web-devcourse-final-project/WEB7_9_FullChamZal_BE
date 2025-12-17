package back.fcz.domain.admin.report.controller;

import back.fcz.domain.admin.report.dto.*;
import back.fcz.domain.admin.report.service.AdminReportService;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reports")
@Tag(name = "Admin-Report", description = "관리자 - 신고 관리 API")
public class AdminReportController {

    private final AdminReportService adminReportService;

    /**
     * 3-1 신고 목록 조회
     */
    @GetMapping
    @Operation(summary = "신고 목록 조회", description = "관리자가 신고 리스트를 페이지네이션/검색으로 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<AdminReportSummaryResponse>>> getReports(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AdminReportSearchRequest cond = AdminReportSearchRequest.of(page, size, status, targetType, from, to);
        PageResponse<AdminReportSummaryResponse> result = adminReportService.getReports(cond);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 3-2 신고 상세 조회
     */
    @GetMapping("/{reportId}")
    @Operation(summary = "신고 상세 조회", description = "관리자가 특정 신고의 상세 정보를 조회합니다.")
    @ApiErrorCodeExample({ErrorCode.ADMIN_REPORT_NOT_FOUND})
    public ResponseEntity<ApiResponse<AdminReportDetailResponse>> getReportDetail(
            @PathVariable Long reportId
    ) {
        AdminReportDetailResponse detail = adminReportService.getReportDetail(reportId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * 3-3 신고 상태 변경
     */
    @PatchMapping("/{reportId}/status")
    @Operation(summary = "신고 상태 변경", description = "관리자가 신고의 상태를 변경하고 필요 시 조치를 수행합니다.")
    @ApiErrorCodeExample({
            ErrorCode.ADMIN_REPORT_NOT_FOUND,
            ErrorCode.ADMIN_REPORT_ALREADY_DONE,
            ErrorCode.ADMIN_INVALID_REPORT_STATUS_CHANGE
    })
    public ResponseEntity<ApiResponse<AdminReportStatusUpdateResponse>> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody @Valid AdminReportStatusUpdateRequest request
    ) {
        AdminReportStatusUpdateResponse response = adminReportService.updateReportStatus(reportId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

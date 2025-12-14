package back.fcz.domain.admin.report.controller;

import back.fcz.domain.admin.report.dto.*;
import back.fcz.domain.admin.report.service.AdminReportService;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    /**
     * 3-1 신고 목록 조회
     */
    @GetMapping
    public ApiResponse<PageResponse<AdminReportSummaryResponse>> getReports(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AdminReportSearchRequest cond = AdminReportSearchRequest.of(
                page, size, status, targetType, from, to
        );

        PageResponse<AdminReportSummaryResponse> result = adminReportService.getReports(cond);
        return ApiResponse.success(result);
    }

    /**
     * 3-2 신고 상세 조회
     */
    @GetMapping("/{reportId}")
    public ApiResponse<AdminReportDetailResponse> getReportDetail(
            @PathVariable Long reportId
    ) {
        AdminReportDetailResponse detail = adminReportService.getReportDetail(reportId);
        return ApiResponse.success(detail);
    }

    /**
     * 3-3 신고 상태 변경
     */
    @PatchMapping("/{reportId}/status")
    public ApiResponse<AdminReportStatusUpdateResponse> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody @Valid AdminReportStatusUpdateRequest request
    ) {
        AdminReportStatusUpdateResponse response =
                adminReportService.updateReportStatus(reportId, request);
        return ApiResponse.success(response);
    }
}

package back.fcz.domain.admin.moderation.controller;

import back.fcz.domain.admin.moderation.dto.AdminModerationAuditLogDetailResponse;
import back.fcz.domain.admin.moderation.dto.AdminModerationAuditLogSearchRequest;
import back.fcz.domain.admin.moderation.dto.AdminModerationAuditLogSummaryResponse;
import back.fcz.domain.admin.moderation.service.AdminModerationAuditLogService;
import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.entity.ModerationDecision;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/moderation-audit-logs")
@Tag(name = "Admin-Moderation", description = "관리자 - AI 유해성 검증 로그 조회 API")
public class AdminModerationAuditLogController {

    private final AdminModerationAuditLogService adminModerationAuditLogService;

    @GetMapping
    @Operation(summary = "AI 검증 로그 목록 조회", description = "관리자가 moderation_audit_log를 페이지네이션/필터로 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<AdminModerationAuditLogSummaryResponse>>> getAuditLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) ModerationDecision decision,
            @RequestParam(required = false) ModerationActionType actionType,
            @RequestParam(required = false) Long actorMemberId,
            @RequestParam(required = false) Long capsuleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        AdminModerationAuditLogSearchRequest req = AdminModerationAuditLogSearchRequest.from(
                page, size, decision, actionType, actorMemberId, capsuleId, from, to
        );

        PageResponse<AdminModerationAuditLogSummaryResponse> result = adminModerationAuditLogService.getAuditLogs(req);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "AI 검증 로그 단건 조회", description = "관리자가 moderation_audit_log 단건을 상세 조회합니다.")
    @ApiErrorCodeExample({
            ErrorCode.ADMIN_MODERATION_LOG_NOT_FOUND
    })
    public ResponseEntity<ApiResponse<AdminModerationAuditLogDetailResponse>> getAuditLog(@PathVariable Long id) {
        AdminModerationAuditLogDetailResponse result = adminModerationAuditLogService.getAuditLog(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

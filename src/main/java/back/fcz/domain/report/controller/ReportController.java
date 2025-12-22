package back.fcz.domain.report.controller;

import back.fcz.domain.report.dto.request.ReportRequest;
import back.fcz.domain.report.service.ReportService;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/report")
@Tag(
        name = "캡슐 신고 API",
        description = "캡슐 신고 생성 API"
)
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(
            summary = "신고 생성",
            description = "캡슐을 신고할 수 있습니다. " +
                    "신고 시 사용할 수 있는 ReportReasonType은 SPAM, OBSCENITY, HATE, FRAUD, ETC가 있습니다."
    )
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_INVALID,
            ErrorCode.CAPSULE_NOT_FOUND
    })
    public ResponseEntity<ApiResponse<Long>> report(
            @RequestBody ReportRequest reportRequest
            ){

        Long reportId = reportService.createReport(reportRequest);
        return ResponseEntity.ok(ApiResponse.success(reportId));
    }
}

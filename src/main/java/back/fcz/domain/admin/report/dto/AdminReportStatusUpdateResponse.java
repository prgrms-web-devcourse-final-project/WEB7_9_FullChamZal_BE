package back.fcz.domain.admin.report.dto;

import back.fcz.domain.report.entity.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminReportStatusUpdateResponse {

    private final Long id;
    private final String status;
    private final String action;

    private final Long processedBy;
    private final LocalDateTime processedAt;
    private final String processMemo;

    public static AdminReportStatusUpdateResponse of(Report report, String action) {
        return AdminReportStatusUpdateResponse.builder()
                .id(report.getId())
                .status(report.getStatus().name())
                .action(action)
                .processedBy(report.getProcessedBy())
                .processedAt(report.getProcessedAt())
                .processMemo(report.getAdminMemo())
                .build();
    }
}

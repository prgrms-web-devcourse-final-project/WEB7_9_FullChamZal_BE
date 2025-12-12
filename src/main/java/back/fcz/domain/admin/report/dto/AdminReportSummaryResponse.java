package back.fcz.domain.admin.report.dto;

import back.fcz.domain.report.entity.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminReportSummaryResponse {

    private final Long id;               // 신고 ID
    private final String targetType;     // CAPSULE / MEMBER
    private final Long targetId;         // 신고 대상 ID (capsuleId or memberId)

    private final Long reporterId;       // 신고자 회원 ID(있다면)
    private final String reporterNickname;

    private final String reasonType;     // ABUSE / SPAM 등
    private final String status;         // PENDING / REVIEWING / ACCEPTED / REJECTED

    private final LocalDateTime createdAt;

    public static AdminReportSummaryResponse from(Report report) {

        Long reporterId = null;
        String reporterNickname = null;

        if (report.getReporter() != null) {
            reporterId = report.getReporter().getMemberId();
            reporterNickname = report.getReporter().getNickname();
        }

        return AdminReportSummaryResponse.builder()
                .id(report.getId())
                .targetType("CAPSULE")
                .targetId(report.getCapsule().getCapsuleId())
                .reporterId(reporterId)
                .reporterNickname(reporterNickname)
                .reasonType(report.getReasonType().name())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .build();
    }
}

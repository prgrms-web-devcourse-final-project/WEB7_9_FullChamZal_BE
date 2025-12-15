package back.fcz.domain.admin.report.dto;

import back.fcz.domain.report.entity.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminReportDetailResponse {

    private final Long id;

    private final String targetType;     // CAPSULE
    private final Long targetId;         // capsule_id
    private final String targetTitle;    // 캡슐 제목
    private final String targetWriterNickname;

    private final String status;         // PENDING / REVIEWING / ACCEPTED / REJECTED
    private final String reasonType;
    private final String reasonDetail;         // 신고 상세 내용

    private final Long reporterId;
    private final String reporterNickname;
    private final String reporterPhone;  // 비회원 신고자 전화번호(암호화된 값 그대로 내려줌)

    // 처리 정보
    private final Long processedBy;      // 처리한 관리자 ID
    private final LocalDateTime processedAt;
    private final String adminMemo;

    private final LocalDateTime createdAt;

    public static AdminReportDetailResponse from(Report report) {

        Long reporterId = null;
        String reporterNickname = null;

        if (report.getReporter() != null) {
            reporterId = report.getReporter().getMemberId();
            reporterNickname = report.getReporter().getNickname();
        }

        return AdminReportDetailResponse.builder()
                .id(report.getId())
                .targetType("CAPSULE")
                .targetId(report.getCapsule().getCapsuleId())
                .targetTitle(report.getCapsule().getTitle())
                .targetWriterNickname(report.getCapsule().getNickname())
                .status(report.getStatus().name())
                .reasonType(report.getReasonType().name())
                .reasonDetail(report.getReasonDetail())
                .reporterId(reporterId)
                .reporterNickname(reporterNickname)
                .reporterPhone(report.getReporterPhone())
                .processedBy(report.getProcessedBy())
                .processedAt(report.getProcessedAt())
                .adminMemo(report.getAdminMemo())
                .createdAt(report.getCreatedAt())
                .build();
    }
}

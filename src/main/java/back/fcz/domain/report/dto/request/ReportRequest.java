package back.fcz.domain.report.dto.request;

import back.fcz.domain.report.entity.ReportReasonType;
import jakarta.validation.constraints.NotBlank;

public record ReportRequest(

        @NotBlank(message = "신고할 캡슐ID는 필수입니다")
        Long capsuleId,

        @NotBlank(message = "신고 유형은 필수입니다")
        ReportReasonType reasonType,

        @NotBlank(message = "신고 상세 사유는 필수입니다")
        String reasonDetail
) {
}

package back.fcz.domain.admin.report.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class AdminReportStatusUpdateRequest {

    @NotBlank
    private String status;   // PENDING / REVIEWING / ACCEPTED / REJECTED

    @NotBlank
    private String action;   // NONE / HIDE_CAPSULE / SUSPEND_MEMBER 등 (enum 예정)

    private String processMemo;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sanctionUntil; // 회원 정지 종료 시각 (선택)
}

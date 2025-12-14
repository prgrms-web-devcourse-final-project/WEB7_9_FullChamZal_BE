package back.fcz.domain.admin.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class AdminReportSearchRequest {

    private final int page;
    private final int size;

    // PENDING / REVIEWING / ACCEPTED / REJECTED
    private final String status;

    // CAPSULE / MEMBER 등 (실제 enum 있으면 String 대신 enum으로 교체 가능)
    private final String targetType;

    private final LocalDate from;   // 신고일 시작
    private final LocalDate to;     // 신고일 끝

    public static AdminReportSearchRequest of(
            Integer page,
            Integer size,
            String status,
            String targetType,
            LocalDate from,
            LocalDate to
    ) {
        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size <= 0) ? 20 : size;

        return AdminReportSearchRequest.builder()
                .page(safePage)
                .size(safeSize)
                .status(status)
                .targetType(targetType)
                .from(from)
                .to(to)
                .build();
    }
}

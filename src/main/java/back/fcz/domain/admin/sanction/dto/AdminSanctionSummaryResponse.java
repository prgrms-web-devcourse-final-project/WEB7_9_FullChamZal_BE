package back.fcz.domain.admin.sanction.dto;

import back.fcz.domain.sanction.entity.MemberSanctionHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminSanctionSummaryResponse {

    private final Long id;
    private final Long memberId;
    private final Long adminId;

    private final String sanctionType;   // STOP / RESTORE / EXIT
    private final String beforeStatus;   // ACTIVE / STOP / EXIT
    private final String afterStatus;

    private final String reason;
    private final LocalDateTime sanctionUntil;

    private final LocalDateTime createdAt;

    public static AdminSanctionSummaryResponse from(MemberSanctionHistory h) {
        return AdminSanctionSummaryResponse.builder()
                .id(h.getId())
                .memberId(h.getMemberId())                         // ✅ Long 필드
                .adminId(h.getAdminId())
                .sanctionType(h.getSanctionType().name())
                .beforeStatus(h.getBeforeStatus().name())          // ✅ MemberStatus → name()
                .afterStatus(h.getAfterStatus().name())
                .reason(h.getReason())
                .sanctionUntil(h.getSanctionUntil())
                .createdAt(h.getCreatedAt())
                .build();
    }
}

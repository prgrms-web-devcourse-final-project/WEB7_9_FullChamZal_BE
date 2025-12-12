// back/fcz/domain/admin/phoneverification/dto/AdminPhoneVerificationSearchRequest.java
package back.fcz.domain.admin.phoneverification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class AdminPhoneVerificationSearchRequest {

    private int page = 0;          // 0-based
    private int size = 20;

    private String purpose;        // SIGNUP / CHANGE_PHONE / GUEST_VERIFY
    private String status;         // PENDING / VERIFIED / EXPIRED

    private LocalDate from;        // 아직 미사용 (추후 Querydsl)
    private LocalDate to;

    public static AdminPhoneVerificationSearchRequest of(
            Integer page,
            Integer size,
            String purpose,
            String status,
            LocalDate from,
            LocalDate to
    ) {
        AdminPhoneVerificationSearchRequest cond = new AdminPhoneVerificationSearchRequest();

        if (page != null && page >= 0) {
            cond.page = page;
        }
        if (size != null && size > 0) {
            cond.size = size;
        }

        cond.purpose = purpose;
        cond.status = status;
        cond.from = from;
        cond.to = to;

        return cond;
    }
}

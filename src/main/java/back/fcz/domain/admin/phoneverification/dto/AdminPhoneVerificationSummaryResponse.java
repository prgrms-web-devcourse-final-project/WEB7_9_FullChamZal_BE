// back/fcz/domain/admin/phoneverification/dto/AdminPhoneVerificationSummaryResponse.java
package back.fcz.domain.admin.phoneverification.dto;

import back.fcz.domain.sms.entity.PhoneVerification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPhoneVerificationSummaryResponse {

    private final Long id;

    private final String phoneNumberHash;  // 검색용 해시

    private final String purpose;          // SIGNUP / CHANGE_PHONE / GUEST_VERIFY
    private final String status;           // PENDING / VERIFIED / EXPIRED

    private final int attemptCount;        // 시도 횟수
    private final LocalDateTime createdAt;
    private final LocalDateTime verifiedAt;
    private final LocalDateTime expiredAt;

    public static AdminPhoneVerificationSummaryResponse from(PhoneVerification entity) {
        return AdminPhoneVerificationSummaryResponse.builder()
                .id(entity.getId())
                .phoneNumberHash(entity.getPhoneNumberHash())
                .purpose(entity.getPurpose().name())
                .status(entity.getStatus().name())
                .attemptCount(entity.getAttemptCount())
                .createdAt(entity.getCreatedAt())
                .verifiedAt(entity.getVerifiedAt())
                .expiredAt(entity.getExpiredAt())
                .build();
    }
}

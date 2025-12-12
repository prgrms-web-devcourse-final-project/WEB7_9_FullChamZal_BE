// back/fcz/domain/admin/phoneverification/dto/AdminPhoneVerificationDetailResponse.java
package back.fcz.domain.admin.phoneverification.dto;

import back.fcz.domain.sms.entity.PhoneVerification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPhoneVerificationDetailResponse {

    private final Long id;

    private final String phoneNumberHash;

    private final String purpose;
    private final String status;

    private final int attemptCount;

    private final LocalDateTime createdAt;
    private final LocalDateTime verifiedAt;
    private final LocalDateTime expiredAt;

    public static AdminPhoneVerificationDetailResponse from(PhoneVerification entity) {
        return AdminPhoneVerificationDetailResponse.builder()
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

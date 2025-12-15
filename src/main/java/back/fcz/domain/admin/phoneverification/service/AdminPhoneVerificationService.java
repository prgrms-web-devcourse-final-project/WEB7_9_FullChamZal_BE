// back/fcz/domain/admin/phoneverification/service/AdminPhoneVerificationService.java
package back.fcz.domain.admin.phoneverification.service;

import back.fcz.domain.admin.phoneverification.dto.AdminPhoneVerificationDetailResponse;
import back.fcz.domain.admin.phoneverification.dto.AdminPhoneVerificationSearchRequest;
import back.fcz.domain.admin.phoneverification.dto.AdminPhoneVerificationSummaryResponse;
import back.fcz.domain.sms.entity.PhoneVerification;
import back.fcz.domain.sms.entity.PhoneVerificationPurpose;
import back.fcz.domain.sms.entity.PhoneVerificationStatus;
import back.fcz.domain.sms.repository.PhoneVerificationRepository;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPhoneVerificationService {

    private final PhoneVerificationRepository phoneVerificationRepository;

    /**
     * 4-1. 전화번호 인증 로그 목록 조회
     */
    public PageResponse<AdminPhoneVerificationSummaryResponse> getPhoneVerifications(
            AdminPhoneVerificationSearchRequest cond
    ) {

        Pageable pageable = PageRequest.of(
                cond.getPage(),
                cond.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        PhoneVerificationPurpose purposeEnum = null;
        PhoneVerificationStatus statusEnum = null;

        if (cond.getPurpose() != null && !cond.getPurpose().isBlank()) {
            try {
                purposeEnum = PhoneVerificationPurpose.valueOf(cond.getPurpose());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }

        if (cond.getStatus() != null && !cond.getStatus().isBlank()) {
            try {
                statusEnum = PhoneVerificationStatus.valueOf(cond.getStatus());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
        }

        Page<PhoneVerification> page;

        if (purposeEnum != null && statusEnum != null) {
            page = phoneVerificationRepository.findByPurposeAndStatus(purposeEnum, statusEnum, pageable);
        } else if (purposeEnum != null) {
            page = phoneVerificationRepository.findByPurpose(purposeEnum, pageable);
        } else if (statusEnum != null) {
            page = phoneVerificationRepository.findByStatus(statusEnum, pageable);
        } else {
            page = phoneVerificationRepository.findAll(pageable);
        }

        Page<AdminPhoneVerificationSummaryResponse> dtoPage =
                page.map(AdminPhoneVerificationSummaryResponse::from);

        return new PageResponse<>(dtoPage);
    }

    /**
     * 4-2. 전화번호 인증 로그 단건 조회
     */
    public AdminPhoneVerificationDetailResponse getPhoneVerificationDetail(Long id) {
        PhoneVerification entity = phoneVerificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_PHONE_VERIFICATION_NOT_FOUND));

        return AdminPhoneVerificationDetailResponse.from(entity);
    }
}

package back.fcz.domain.admin.capsule.service;

import back.fcz.domain.admin.capsule.dto.AdminCapsuleDeleteRequest;
import back.fcz.domain.admin.capsule.dto.AdminCapsuleDetailResponse;
import back.fcz.domain.admin.capsule.dto.AdminCapsuleSummaryResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
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
public class AdminCapsuleService {

    private final CapsuleRepository capsuleRepository;

    /**
     * 캡슐 목록 조회 (getCapsules)
     */
    public PageResponse<AdminCapsuleSummaryResponse> getCapsules(
            int page,
            int size,
            String visibility   // PUBLIC / PRIVATE, null 이면 전체
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Capsule> capsulePage;

        if (visibility != null && !visibility.isBlank()) {
            capsulePage = capsuleRepository.findByIsDeletedFalseAndVisibility(visibility, pageable);
        } else {
            capsulePage = capsuleRepository.findByIsDeletedFalse(pageable);
        }

        Page<AdminCapsuleSummaryResponse> dtoPage =
                capsulePage.map(AdminCapsuleSummaryResponse::from);

        return new PageResponse<>(dtoPage);
    }

    /**
     * 캡슐 상세 조회 (getCapsuleDetail)
     */
    public AdminCapsuleDetailResponse getCapsuleDetail(Long capsuleId) {
        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_CAPSULE_NOT_FOUND));

        return AdminCapsuleDetailResponse.from(capsule);
    }

    /**
     * 캡슐 삭제/복구 (updateCapsuleDeleted)
     */
    @Transactional
    public AdminCapsuleDetailResponse updateCapsuleDeleted(Long capsuleId,
                                                           AdminCapsuleDeleteRequest request) {
        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_CAPSULE_NOT_FOUND));

        boolean wantDelete = request.getDeleted();

        // 이미 상태가 같은 경우 예외를 던질지 말지는 팀 규칙에 맞게
//        if (capsule.isDeleted() == wantDelete) {
//            // 굳이 에러로 보고 싶으면 ErrorCode 하나 더 추가해서 던지면 됨
//            // throw new BusinessException(ErrorCode.ADMIN_CAPSULE_INVALID_STATUS_CHANGE);
//            return AdminCapsuleDetailResponse.from(capsule);
//        }

        // TODO: Capsule 엔티티에 setter(or 도메인 메서드) 추가 필요
        //capsule.setDeleted(wantDelete); //

        // TODO: request.getReason() 는 나중에 제재 로그 테이블이 생기면 같이 저장

        return AdminCapsuleDetailResponse.from(capsule);
    }
}

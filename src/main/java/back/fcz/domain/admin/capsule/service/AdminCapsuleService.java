package back.fcz.domain.admin.capsule.service;

import back.fcz.domain.admin.capsule.dto.AdminCapsuleDeleteRequest;
import back.fcz.domain.admin.capsule.dto.AdminCapsuleDetailResponse;
import back.fcz.domain.admin.capsule.dto.AdminCapsuleSummaryResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.domain.report.repository.ReportRepository;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCapsuleService {

    private final CapsuleRepository capsuleRepository;
    private final ReportRepository reportRepository;

    public PageResponse<AdminCapsuleSummaryResponse> getCapsules(
            Integer page,
            Integer size,
            String visibility,
            Boolean deleted,
            String keyword
    ) {
        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size <= 0) ? 20 : size;

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Capsule> capsules = capsuleRepository.searchAdmin(visibility, deleted, keyword, pageable);

        // reportCount 배치 집계
        List<Long> capsuleIds = capsules.getContent().stream().map(Capsule::getCapsuleId).toList();
        Map<Long, Long> reportCountMap = toCountMap(reportRepository.countByCapsuleIds(capsuleIds));

        Page<AdminCapsuleSummaryResponse> dtoPage = capsules.map(c ->
                AdminCapsuleSummaryResponse.from(
                        c,
                        reportCountMap.getOrDefault(c.getCapsuleId(), 0L),
                        0L // TODO: bookmark
                )
        );

        return new PageResponse<>(dtoPage);
    }

    public AdminCapsuleDetailResponse getCapsuleDetail(Long capsuleId) {
        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_CAPSULE_NOT_FOUND));

        long reportCount = reportRepository.countByCapsule_CapsuleId(capsuleId);

        return AdminCapsuleDetailResponse.from(capsule, reportCount, 0L);
    }

    @Transactional
    public AdminCapsuleDetailResponse updateCapsuleDeleted(Long capsuleId, AdminCapsuleDeleteRequest request) {
        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_CAPSULE_NOT_FOUND));

        boolean wantDeleted = Boolean.TRUE.equals(request.getDeleted());
        boolean currentlyDeleted = capsule.getIsDeleted() != 0;

        if (wantDeleted == currentlyDeleted) {
            throw new BusinessException(ErrorCode.ADMIN_INVALID_CAPSULE_STATUS_CHANGE);
        }

        if (wantDeleted) {
            capsule.setIsDeleted(2); // 관리자 삭제(정책)
            capsule.markDeleted();
        } else {
            capsule.setIsDeleted(0);
            capsule.clearDeletedAt();
        }

        long reportCount = reportRepository.countByCapsule_CapsuleId(capsuleId);

        return AdminCapsuleDetailResponse.from(capsule, reportCount, 0L);
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            Long cnt = (Long) row[1];
            map.put(id, cnt);
        }
        return map;
    }
}

package back.fcz.domain.admin.capsule.service;

import back.fcz.domain.admin.capsule.dto.AdminCapsuleDeleteRequest;
import back.fcz.domain.admin.capsule.dto.AdminCapsuleDetailResponse;
import back.fcz.domain.admin.capsule.dto.AdminCapsuleSummaryResponse;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.entity.CapsuleRecipient;
import back.fcz.domain.capsule.repository.CapsuleRecipientRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCapsuleService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final ReportRepository reportRepository;

    /**
     * 관리자 캡슐 목록 조회 (검색/필터 포함)
     */
    public PageResponse<AdminCapsuleSummaryResponse> getCapsules(
            int page,
            int size,
            String visibility,
            Integer isDeleted,
            Integer isProtected,
            String keyword
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size <= 0 ? 20 : size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Capsule> capsulePage = capsuleRepository.searchAdmin(
                visibility,
                isDeleted,
                isProtected,
                keyword,
                pageable
        );

        List<Long> capsuleIds = capsulePage.getContent().stream()
                .map(Capsule::getCapsuleId)
                .toList();

        Map<Long, String> recipientNameMap = fetchRecipientNameMap(capsuleIds);
        Map<Long, Long> reportCountMap = fetchReportCountMap(capsuleIds);

        Page<AdminCapsuleSummaryResponse> mapped = capsulePage.map(c -> {
            String recipientName = "PRIVATE".equalsIgnoreCase(c.getVisibility())
                    ? recipientNameMap.get(c.getCapsuleId())
                    : null;

            long reportCount = reportCountMap.getOrDefault(c.getCapsuleId(), 0L);

            return AdminCapsuleSummaryResponse.of(
                    c,
                    recipientName,
                    reportCount,
                    0L // TODO bookmark
            );
        });

        return new PageResponse<>(mapped);
    }

    /**
     * 관리자 캡슐 상세 조회
     */
    public AdminCapsuleDetailResponse getCapsuleDetail(Long capsuleId) {
        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_CAPSULE_NOT_FOUND));

        String recipientName = null;
        if ("PRIVATE".equalsIgnoreCase(capsule.getVisibility())) {
            recipientName = capsuleRecipientRepository.findByCapsuleId_CapsuleId(capsuleId)
                    .map(CapsuleRecipient::getRecipientName)
                    .orElse(null);
        }

        long reportCount = reportRepository.countByCapsuleIds(List.of(capsuleId)).stream()
                .findFirst()
                .map(row -> (Long) row[1])
                .orElse(0L);

        return AdminCapsuleDetailResponse.of(
                capsule,
                recipientName,
                reportCount,
                0L // TODO bookmark
        );
    }

    /**
     * 관리자 캡슐 삭제/복구
     * - 삭제: isDeleted=2 + deletedAt 기록
     * - 복구: isDeleted=0 + deletedAt null
     */
    @Transactional
    public AdminCapsuleDetailResponse updateCapsuleDeleted(Long capsuleId, AdminCapsuleDeleteRequest request) {
        Capsule capsule = capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_CAPSULE_NOT_FOUND));

        if (Boolean.TRUE.equals(request.getDeleted())) {
            capsule.setIsDeleted(2);
            capsule.markDeleted();
        } else {
            capsule.setIsDeleted(0);
            capsule.clearDeletedAt();
        }

        // 상세 응답은 항상 최신 기준으로 재구성
        return getCapsuleDetail(capsuleId);
    }

    private Map<Long, String> fetchRecipientNameMap(List<Long> capsuleIds) {
        if (capsuleIds == null || capsuleIds.isEmpty()) return Collections.emptyMap();

        List<CapsuleRecipient> recipients = capsuleRecipientRepository.findAllByCapsuleIds(capsuleIds);

        return recipients.stream()
                .filter(r -> r.getCapsuleId() != null)
                .collect(Collectors.toMap(
                        r -> r.getCapsuleId().getCapsuleId(),
                        CapsuleRecipient::getRecipientName,
                        (a, b) -> a
                ));
    }

    private Map<Long, Long> fetchReportCountMap(List<Long> capsuleIds) {
        if (capsuleIds == null || capsuleIds.isEmpty()) return Collections.emptyMap();

        List<Object[]> rows = reportRepository.countByCapsuleIds(capsuleIds);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            Long capsuleId = (Long) row[0];
            Long cnt = (Long) row[1];
            map.put(capsuleId, cnt);
        }
        return map;
    }
}

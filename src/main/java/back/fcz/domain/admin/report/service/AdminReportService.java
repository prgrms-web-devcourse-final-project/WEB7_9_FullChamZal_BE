package back.fcz.domain.admin.report.service;

import back.fcz.domain.admin.report.dto.*;
import back.fcz.domain.report.entity.Report;
import back.fcz.domain.report.entity.ReportStatus;
import back.fcz.domain.report.repository.ReportRepository;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private final ReportRepository reportRepository;

    /**
     * 3-1 신고 목록 조회
     */
    public PageResponse<AdminReportSummaryResponse> getReports(AdminReportSearchRequest cond) {

        Pageable pageable = PageRequest.of(
                cond.getPage(),
                cond.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // TODO: status / 기간 / targetType 필터링은 Querydsl로 확장 예정
        Page<Report> page = reportRepository.findAll(pageable);

        Page<AdminReportSummaryResponse> dtoPage =
                page.map(AdminReportSummaryResponse::from);

        return new PageResponse<>(dtoPage);
    }

    /**
     * 3-2 신고 상세 조회
     */
    public AdminReportDetailResponse getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_REPORT_NOT_FOUND));

        return AdminReportDetailResponse.from(report);
    }

    /**
     * 3-3 신고 상태 변경
     */
    @Transactional
    public AdminReportStatusUpdateResponse updateReportStatus(
            Long reportId,
            AdminReportStatusUpdateRequest request
    ) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_REPORT_NOT_FOUND));

        // 이미 처리 완료된 신고인지 검증
        if (report.isDone()) {
            throw new BusinessException(ErrorCode.ADMIN_REPORT_ALREADY_DONE);
        }

        // 문자열 status -> enum 변환
        ReportStatus newStatus;
        try {
            newStatus = ReportStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED); // 이미 있는 공통 코드 사용
        }

        LocalDateTime now = LocalDateTime.now();

        // TODO: 실제로는 SecurityContextHolder에서 adminId 꺼내오기
        Long adminId = 0L; // 임시 값, 나중에 교체

        // 간단 로직: 상태에 따라 도메인 메서드 사용
        switch (newStatus) {
            case REVIEWING -> report.startReview(adminId);
            case ACCEPTED  -> report.accept(adminId, request.getProcessMemo(), now);
            case REJECTED  -> report.reject(adminId, request.getProcessMemo(), now);
            case PENDING   -> {
                // (필요 없다면 막아도 됨)
                report.reject(adminId, request.getProcessMemo(), now);
            }
        }

        // TODO: request.getAction() 에 따라 캡슐 숨김 / 회원 정지 같은 추가 조치 연동은 이후 단계에서 구현

        return AdminReportStatusUpdateResponse.of(report, request.getAction());
    }
}

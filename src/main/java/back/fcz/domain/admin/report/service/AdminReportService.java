package back.fcz.domain.admin.report.service;

import back.fcz.domain.admin.report.dto.*;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.domain.report.entity.Report;
import back.fcz.domain.report.entity.ReportStatus;
import back.fcz.domain.report.repository.ReportRepository;
import back.fcz.domain.sanction.entity.MemberSanctionHistory;
import back.fcz.domain.sanction.entity.SanctionType;
import back.fcz.domain.sanction.repository.MemberSanctionHistoryRepository;
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
    private final MemberSanctionHistoryRepository memberSanctionHistoryRepository;
    private final CurrentUserContext currentUserContext;

    public PageResponse<AdminReportSummaryResponse> getReports(AdminReportSearchRequest cond) {
        ReportStatus status = parseReportStatusOrNull(cond.getStatus());

        LocalDateTime from = (cond.getFrom() == null) ? null : cond.getFrom().atStartOfDay();
        LocalDateTime to = (cond.getTo() == null) ? null : cond.getTo().plusDays(1).atStartOfDay();

        Pageable pageable = PageRequest.of(
                cond.getPage(),
                cond.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Report> page = reportRepository.searchAdmin(status, from, to, pageable);
        Page<AdminReportSummaryResponse> dtoPage = page.map(AdminReportSummaryResponse::from);

        return new PageResponse<>(dtoPage);
    }

    public AdminReportDetailResponse getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_REPORT_NOT_FOUND));

        return AdminReportDetailResponse.from(report);
    }

    @Transactional
    public AdminReportStatusUpdateResponse updateReportStatus(Long reportId, AdminReportStatusUpdateRequest req) {
        Long adminId = currentUserContext.getCurrentMemberId();

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_REPORT_NOT_FOUND));

        if (report.isDone()) {
            throw new BusinessException(ErrorCode.ADMIN_REPORT_ALREADY_DONE);
        }

        ReportStatus newStatus = parseReportStatus(req.getStatus());
        ReportAction action = ReportAction.from(req.getAction());

        // 상태 전이 규칙(간단 버전)
        if (newStatus == ReportStatus.PENDING) {
            throw new BusinessException(ErrorCode.ADMIN_INVALID_REPORT_STATUS_CHANGE);
        }
        if (newStatus == ReportStatus.REVIEWING) {
            if (report.getStatus() != ReportStatus.PENDING) {
                throw new BusinessException(ErrorCode.ADMIN_INVALID_REPORT_STATUS_CHANGE);
            }
            report.startReview(adminId);
            return AdminReportStatusUpdateResponse.of(report, action.name());
        }

        // ACCEPTED / REJECTED
        if (newStatus == ReportStatus.ACCEPTED) {
            report.accept(adminId, req.getProcessMemo(), LocalDateTime.now());
            applyActionIfNeeded(report, adminId, action, req);
        } else if (newStatus == ReportStatus.REJECTED) {
            report.reject(adminId, req.getProcessMemo(), LocalDateTime.now());
        } else {
            throw new BusinessException(ErrorCode.ADMIN_INVALID_REPORT_STATUS_CHANGE);
        }

        return AdminReportStatusUpdateResponse.of(report, action.name());
    }

    private void applyActionIfNeeded(Report report, Long adminId, ReportAction action, AdminReportStatusUpdateRequest req) {
        Capsule capsule = report.getCapsule();
        if (capsule == null) {
            return;
        }

        switch (action) {
            case NONE -> { /* nothing */ }

            case HIDE_CAPSULE -> capsule.setProtected(1);

            case UNHIDE_CAPSULE -> capsule.setProtected(0);

            case DELETE_CAPSULE -> {
                capsule.setIsDeleted(2);
                capsule.markDeleted();
            }

            case RESTORE_CAPSULE -> {
                capsule.setIsDeleted(0);
                capsule.clearDeletedAt();
            }

            case SUSPEND_MEMBER -> {
                Member writer = capsule.getMemberId();
                if (writer == null) return;

                MemberStatus before = writer.getStatus();
                if (before == MemberStatus.EXIT) {
                    throw new BusinessException(ErrorCode.ADMIN_INVALID_MEMBER_STATUS_CHANGE);
                }

                writer.updateStatus(MemberStatus.STOP);

                memberSanctionHistoryRepository.save(
                        MemberSanctionHistory.create(
                                writer.getMemberId(),
                                adminId,
                                SanctionType.STOP,
                                before,
                                MemberStatus.STOP,
                                memoOrDefault(req.getProcessMemo(), "REPORT_ACCEPTED_SUSPEND_MEMBER"),
                                req.getSanctionUntil()
                        )
                );
            }

            case RESTORE_MEMBER -> {
                Member writer = capsule.getMemberId();
                if (writer == null) return;

                MemberStatus before = writer.getStatus();
                if (before == MemberStatus.EXIT) {
                    throw new BusinessException(ErrorCode.ADMIN_INVALID_MEMBER_STATUS_CHANGE);
                }

                writer.updateStatus(MemberStatus.ACTIVE);

                memberSanctionHistoryRepository.save(
                        MemberSanctionHistory.create(
                                writer.getMemberId(),
                                adminId,
                                SanctionType.RESTORE,
                                before,
                                MemberStatus.ACTIVE,
                                memoOrDefault(req.getProcessMemo(), "REPORT_ACCEPTED_RESTORE_MEMBER"),
                                req.getSanctionUntil()
                        )
                );
            }
        }
    }

    private static String memoOrDefault(String memo, String def) {
        return (memo == null || memo.isBlank()) ? def : memo;
    }

    private static ReportStatus parseReportStatusOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return parseReportStatus(raw);
    }

    private static ReportStatus parseReportStatus(String raw) {
        try {
            return ReportStatus.valueOf(raw);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ADMIN_INVALID_REPORT_STATUS_CHANGE);
        }
    }

    private enum ReportAction {
        NONE,
        HIDE_CAPSULE,
        UNHIDE_CAPSULE,
        DELETE_CAPSULE,
        RESTORE_CAPSULE,
        SUSPEND_MEMBER,
        RESTORE_MEMBER;

        static ReportAction from(String raw) {
            if (raw == null || raw.isBlank()) return NONE;
            try {
                return ReportAction.valueOf(raw);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.ADMIN_INVALID_REPORT_STATUS_CHANGE);
            }
        }
    }
}

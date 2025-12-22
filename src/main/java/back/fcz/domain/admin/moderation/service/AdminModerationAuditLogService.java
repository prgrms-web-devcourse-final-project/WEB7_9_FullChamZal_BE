package back.fcz.domain.admin.moderation.service;

import back.fcz.domain.admin.moderation.dto.AdminModerationAuditLogDetailResponse;
import back.fcz.domain.admin.moderation.dto.AdminModerationAuditLogSearchRequest;
import back.fcz.domain.admin.moderation.dto.AdminModerationAuditLogSummaryResponse;
import back.fcz.domain.openai.moderation.entity.ModerationAuditLog;
import back.fcz.domain.openai.moderation.repository.ModerationAuditLogRepository;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminModerationAuditLogService {

    private final ModerationAuditLogRepository auditLogRepository;

    public PageResponse<AdminModerationAuditLogSummaryResponse> getAuditLogs(AdminModerationAuditLogSearchRequest req) {
        Page<ModerationAuditLog> page = auditLogRepository.search(
                req.getDecision(),
                req.getActionType(),
                req.getActorMemberId(),
                req.getCapsuleId(),
                req.getFrom(),
                req.getTo(),
                req.toPageable()
        );

        Page<AdminModerationAuditLogSummaryResponse> dtoPage =
                page.map(AdminModerationAuditLogSummaryResponse::from);

        return new PageResponse<>(dtoPage);
    }

    public AdminModerationAuditLogDetailResponse getAuditLog(Long id) {
        ModerationAuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MODERATION_LOG_NOT_FOUND));

        return AdminModerationAuditLogDetailResponse.from(log);
    }
}

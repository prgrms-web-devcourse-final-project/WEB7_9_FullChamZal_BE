package back.fcz.domain.openai.moderation.service;

import back.fcz.domain.openai.moderation.entity.ModerationAuditLog;
import back.fcz.domain.openai.moderation.repository.ModerationAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModerationAuditLogWriter {

    private final ModerationAuditLogRepository auditLogRepository;

    // 실패 로그는 예외가 터져도 DB에 남아야 하므로 "새 트랜잭션"으로 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long saveAndReturnId(ModerationAuditLog log) {
        return auditLogRepository.save(log).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attachCapsuleId(Long auditId, Long capsuleId) {
        if (auditId == null || capsuleId == null) return;
        auditLogRepository.attachCapsuleId(auditId, capsuleId);
    }
}

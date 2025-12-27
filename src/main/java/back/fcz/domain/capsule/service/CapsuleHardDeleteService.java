package back.fcz.domain.capsule.service;

import back.fcz.domain.bookmark.repository.BookmarkRepository;
import back.fcz.domain.capsule.repository.*;
import back.fcz.domain.openai.moderation.repository.ModerationAuditLogRepository;
import back.fcz.domain.report.repository.ReportRepository;
import back.fcz.domain.storytrack.repository.StorytrackStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CapsuleHardDeleteService {

    private final CapsuleRepository capsuleRepository;
    private final CapsuleAttachmentRepository capsuleAttachmentRepository;
    private final CapsuleLikeRepository capsuleLikeRepository;
    private final CapsuleOpenLogRepository capsuleOpenLogRepository;
    private final CapsuleRecipientRepository capsuleRecipientRepository;
    private final PublicCapsuleRecipientRepository publicCapsuleRecipientRepository;

    private final BookmarkRepository bookmarkRepository;
    private final ModerationAuditLogRepository moderationAuditLogRepository;
    private final ReportRepository reportRepository;

    private final StorytrackStepRepository storytrackStepRepository;

    /**
     * 스케줄러/테스트에서 호출하는 1회 실행 메서드
     * @return 실제 삭제된 캡슐 개수
     */
    @Transactional
    public int hardDeleteOnce(int limit) {
        int size = Math.min(limit, 100);
        LocalDateTime now = LocalDateTime.now();

        List<Long> capsuleIds = capsuleRepository.findHardDeleteCandidateIds(now, PageRequest.of(0, size));
        if (capsuleIds.isEmpty()) {
            return 0;
        }

        // 1) 자식(연관) 먼저 삭제
        // 스토리트랙 step (capsule FK)
        storytrackStepRepository.deleteByCapsuleIds(capsuleIds);

        // capsule 연관
        publicCapsuleRecipientRepository.deleteByCapsuleIds(capsuleIds);
        capsuleRecipientRepository.deleteByCapsuleIds(capsuleIds);
        capsuleOpenLogRepository.deleteByCapsuleIds(capsuleIds);
        capsuleLikeRepository.deleteByCapsuleIds(capsuleIds);

        // 기타 연관(캡슐ID 참조)
        bookmarkRepository.deleteByCapsuleIdIn(capsuleIds);
        moderationAuditLogRepository.deleteByCapsuleIdIn(capsuleIds);
        reportRepository.deleteByCapsuleIds(capsuleIds);

        // 2) 마지막에 Capsule 하드 딜리트
        int deleted = capsuleRepository.hardDeleteByCapsuleIds(capsuleIds);

        log.info("[CapsuleHardDelete] deleted={} (requestedIds={})", deleted, capsuleIds.size());
        return deleted;
    }
}

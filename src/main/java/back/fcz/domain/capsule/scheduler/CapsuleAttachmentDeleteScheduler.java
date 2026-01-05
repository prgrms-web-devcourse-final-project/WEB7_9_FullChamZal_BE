package back.fcz.domain.capsule.scheduler;

import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.entity.CapsuleAttachmentStatus;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
import back.fcz.infra.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class CapsuleAttachmentDeleteScheduler {

    private final CapsuleAttachmentRepository capsuleAttachmentRepository;
    private final FileStorage fileStorage;

    @Transactional
    @Scheduled(cron = "0 0 4 * * *") //
    public void markExpiredTempAsDeleted(){
        LocalDateTime now = LocalDateTime.now();

        List<CapsuleAttachment> expired = capsuleAttachmentRepository.findTop1000ByStatusAndExpiredAtBeforeOrderByIdAsc(
                CapsuleAttachmentStatus.TEMP,
                now
        );

        if(expired.isEmpty()) return ;

        expired.forEach(CapsuleAttachment::markDeleted);
        capsuleAttachmentRepository.saveAll(expired);

        log.info("[CapsuleAttachmentDeleteScheduler] TEMP -> DELETED: {}건", expired.size());
    }

    @Transactional
    @Scheduled(cron = "0 10 4 * * *")
    public void markExpiredUploadingOrPendingAsDeleted() {
        LocalDateTime now = LocalDateTime.now();

        // 예: createdAt 기준 30분 초과 UPLOADING 정리
        List<CapsuleAttachment> uploadingExpired =
                capsuleAttachmentRepository.findTop1000ByStatusAndCreatedAtBeforeOrderByIdAsc(
                        CapsuleAttachmentStatus.UPLOADING,
                        now.minusMinutes(30)
                );

        // 예: 20분 초과 PENDING
        List<CapsuleAttachment> pendingExpired =
                capsuleAttachmentRepository.findTop1000ByStatusAndExpiredAtBeforeOrderByIdAsc(
                        CapsuleAttachmentStatus.PENDING,
                        now.minusMinutes(20)
                );

        uploadingExpired.forEach(CapsuleAttachment::markDeleted);
        pendingExpired.forEach(CapsuleAttachment::markDeleted);

        capsuleAttachmentRepository.saveAll(uploadingExpired);
        capsuleAttachmentRepository.saveAll(pendingExpired);

        log.info("[CapsuleAttachmentDeleteScheduler] UPLOADING->DELETED: {}, PENDING->DELETED: {}",
                uploadingExpired.size(), pendingExpired.size());
    }

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void hardDeletedFromS3() {
        List<CapsuleAttachment> targets =
                capsuleAttachmentRepository.findTop1000ByStatusOrderByDeletedAtAsc(
                        CapsuleAttachmentStatus.DELETED
                );
        if (targets.isEmpty()) return;

        List<Long> successIds = new java.util.ArrayList<>();

        for (CapsuleAttachment att : targets) {
            try {
                fileStorage.delete(att.getS3Key());
                successIds.add(att.getId());
            } catch (Exception e) {
                log.error("[CapsuleAttachmentDeleteScheduler] S3 삭제 실패 - attachmentId: {}, s3Key: {}, error: {}",
                        att.getId(), att.getS3Key(), e.getMessage());
            }
        }

        if (!successIds.isEmpty()) {
            capsuleAttachmentRepository.deleteAllByIdInBatch(successIds);
            log.info("[CapsuleAttachmentDeleteScheduler] hard deleted: {}", successIds.size());
        }
    }

}

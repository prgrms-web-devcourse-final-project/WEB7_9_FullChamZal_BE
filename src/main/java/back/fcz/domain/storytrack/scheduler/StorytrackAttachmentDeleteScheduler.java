package back.fcz.domain.storytrack.scheduler;

import back.fcz.domain.storytrack.entity.StorytrackAttachment;
import back.fcz.domain.storytrack.entity.StorytrackStatus;
import back.fcz.domain.storytrack.repository.StorytrackAttachmentRepository;
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
public class StorytrackAttachmentDeleteScheduler {

    private final StorytrackAttachmentRepository storytrackAttachmentRepository;
    private final FileStorage fileStorage;

    @Transactional
    @Scheduled(cron = "0 0 4 * * *") //
    public void markExpiredTempAsDeleted(){
        LocalDateTime now = LocalDateTime.now();

        List<StorytrackAttachment> expired = storytrackAttachmentRepository.findTop1000ByStatusAndExpiredAtBeforeOrderByIdAsc(
                StorytrackStatus.TEMP,
                now
        );

        if(expired.isEmpty()) return ;

        expired.forEach(StorytrackAttachment::markDeleted);
        storytrackAttachmentRepository.saveAll(expired);

        log.info("[StorytrackAttachmentDeleteScheduler] TEMP -> DELETED: {}건", expired.size());
    }

    @Transactional
    @Scheduled(cron = "0 10 4 * * *")
    public void markExpiredUploadingOrPendingAsDeleted() {
        LocalDateTime now = LocalDateTime.now();

        // 예: createdAt 기준 30분 초과 UPLOADING 정리
        List<StorytrackAttachment> uploadingExpired =
                storytrackAttachmentRepository.findTop1000ByStatusAndCreatedAtBeforeOrderByIdAsc(
                        StorytrackStatus.UPLOADING,
                        now.minusMinutes(30)
                );

        // 예: 20분 초과 PENDING
        List<StorytrackAttachment> pendingExpired =
                storytrackAttachmentRepository.findTop1000ByStatusAndExpiredAtBeforeOrderByIdAsc(
                        StorytrackStatus.PENDING,
                        now.minusMinutes(20)
                );

        uploadingExpired.forEach(StorytrackAttachment::markDeleted);
        pendingExpired.forEach(StorytrackAttachment::markDeleted);

        storytrackAttachmentRepository.saveAll(uploadingExpired);
        storytrackAttachmentRepository.saveAll(pendingExpired);

        log.info("[StorytrackAttachmentDeleteScheduler] UPLOADING->DELETED: {}, PENDING->DELETED: {}",
                uploadingExpired.size(), pendingExpired.size());
    }

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void hardDeletedFromS3() {
        List<StorytrackAttachment> targets =
                storytrackAttachmentRepository.findTop1000ByStatusOrderByDeletedAtAsc(
                        StorytrackStatus.DELETED
                );
        if (targets.isEmpty()) return;

        List<Long> successIds = new java.util.ArrayList<>();

        for (StorytrackAttachment att : targets) {
            try {
                fileStorage.delete(att.getS3Key());
                successIds.add(att.getId());
            } catch (Exception e) {
                log.error("[StorytrackAttachmentDeleteScheduler] S3 삭제 실패 - attachmentId: {}, s3Key: {}, error: {}",
                        att.getId(), att.getS3Key(), e.getMessage());
            }
        }

        if (!successIds.isEmpty()) {
            storytrackAttachmentRepository.deleteAllByIdInBatch(successIds);
            log.info("[StorytrackAttachmentDeleteScheduler] hard deleted: {}", successIds.size());
        }
    }
}

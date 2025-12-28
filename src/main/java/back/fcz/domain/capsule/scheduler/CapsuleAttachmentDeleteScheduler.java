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

    @Scheduled(cron = "0 30 4 * * *") // temp -> deleted 작업 후 30분 뒤 실행
    @Transactional
    public void hardDeletedFromS3(){
        List<CapsuleAttachment> targets = capsuleAttachmentRepository.findTop1000ByStatusOrderByIdAsc(
                CapsuleAttachmentStatus.DELETED
        );
        if (targets.isEmpty()) return ;

        for(CapsuleAttachment att : targets){
            try{
                fileStorage.delete(att.getS3Key());
                capsuleAttachmentRepository.delete(att);
            } catch (Exception e){
                log.error("[CapsuleAttachmentDeleteScheduler] S3 삭제 실패 - attachmentId: {}, s3Key: {}, error: {}",
                        att.getId(),
                        att.getS3Key(),
                        e.getMessage()
                );
            }
        }
    }
}

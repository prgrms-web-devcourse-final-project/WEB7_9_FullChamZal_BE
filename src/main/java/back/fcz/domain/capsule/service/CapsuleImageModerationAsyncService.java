package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.entity.CapsuleAttachmentStatus;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.service.CapsuleImageModerationService;
import back.fcz.global.exception.BusinessException;
import back.fcz.infra.storage.PresignedUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CapsuleImageModerationAsyncService {

    private final CapsuleAttachmentRepository capsuleAttachmentRepository;
    private final PresignedUrlProvider presignedUrlProvider;
    private final CapsuleImageModerationService capsuleImageModerationService;

    private static final Duration GET_EXPIRES = Duration.ofMinutes(5);

    @Async
    @Transactional
    public void moderateAsync(Long attachmentId, Long uploaderId){
        CapsuleAttachment attachment = capsuleAttachmentRepository.findById(attachmentId)
                .orElse(null);
        if (attachment == null) return;

        // 멱등성 처리
        if (!attachment.getStatus().equals(CapsuleAttachmentStatus.PENDING)) return;

        String url = presignedUrlProvider.presignGet(attachment.getS3Key(), GET_EXPIRES);

        try {
            capsuleImageModerationService.validateImageUrl(
                    uploaderId,
                    ModerationActionType.CAPSULE_CREATE,
                    url
            );
            attachment.markTemp();
        } catch (BusinessException be) {
            attachment.markDeleted();
        } catch (Exception e) {
            attachment.markDeleted();
        }
    }
}

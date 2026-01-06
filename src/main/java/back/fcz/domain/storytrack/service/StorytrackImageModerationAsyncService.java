package back.fcz.domain.storytrack.service;

import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.service.CapsuleImageModerationService;
import back.fcz.domain.storytrack.entity.StorytrackAttachment;
import back.fcz.domain.storytrack.entity.StorytrackStatus;
import back.fcz.domain.storytrack.repository.StorytrackAttachmentRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.infra.storage.PresignedUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class StorytrackImageModerationAsyncService {

    private final StorytrackAttachmentRepository storytrackAttachmentRepository;
    private final PresignedUrlProvider presignedUrlProvider;
    private final CapsuleImageModerationService storytrackImageModerationService;

    private static final Duration GET_EXPIRES = Duration.ofMinutes(5);

    @Async
    @Transactional
    public void moderateAsync(Long attachmentId, Long uploaderId){
        StorytrackAttachment attachment = storytrackAttachmentRepository.findById(attachmentId)
                .orElse(null);
        if (attachment == null) return;

        // 멱등성 처리
        if (!attachment.getStatus().equals(StorytrackStatus.PENDING)) return;

        String url = presignedUrlProvider.presignGet(attachment.getS3Key(), GET_EXPIRES);

        try {
            storytrackImageModerationService.validateImageUrl(
                    uploaderId,
                    ModerationActionType.STORYTRACK_CREATE,
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

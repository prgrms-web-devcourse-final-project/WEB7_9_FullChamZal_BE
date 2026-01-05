package back.fcz.domain.storytrack.service;

import back.fcz.domain.storytrack.entity.StorytrackAttachment;
import back.fcz.domain.storytrack.entity.StorytrackStatus;
import back.fcz.domain.storytrack.repository.StorytrackAttachmentRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StorytrackAttachmentService {
    private final StorytrackAttachmentRepository storytrackAttachmentRepository;



    @Transactional
    public void deleteTemp(Long memberId, Long attachmentId){
        StorytrackAttachment attachment = storytrackAttachmentRepository.findByIdAndUploaderIdAndStatusAndDeletedAtIsNull(attachmentId, memberId, StorytrackStatus.TEMP)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_FILE_DELETE_FORBIDDEN));

        attachment.markDeleted();
        storytrackAttachmentRepository.save(attachment);
    }
}

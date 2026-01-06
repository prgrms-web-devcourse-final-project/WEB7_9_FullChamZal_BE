package back.fcz.domain.storytrack.service;

import back.fcz.domain.storytrack.dto.request.StorytrackAttachmentUploadRequest;
import back.fcz.domain.storytrack.dto.response.StorytrackAttachmentStatusResponse;
import back.fcz.domain.storytrack.dto.response.StorytrackAttachmentUploadResponse;
import back.fcz.domain.storytrack.entity.StorytrackAttachment;
import back.fcz.domain.storytrack.entity.StorytrackStatus;
import back.fcz.domain.storytrack.repository.StorytrackAttachmentRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.infra.storage.PresignedUrlProvider;
import back.fcz.infra.storage.StoredObjectMetadata;
import back.fcz.infra.storage.StoredObjectReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorytrackAttachmentPresignService {

    private final PresignedUrlProvider presignedUrlProvider;
    private final StorytrackAttachmentRepository storytrackAttachmentRepository;
    private final StorytrackImageModerationAsyncService StorytrackAttachmentImageModerationAsyncService;
    private final StoredObjectReader storedObjectReader;

    private static final Duration PUT_EXPIRES = Duration.ofMinutes(5);


    @Transactional
    public StorytrackAttachmentUploadResponse presignedUpload(Long uploaderId, StorytrackAttachmentUploadRequest request){
        String key = generateKey(request.filename(), uploaderId);

        StorytrackAttachment attachment = StorytrackAttachment.createUploading(
                uploaderId,
                key,
                request.filename(),
                request.size(),
                request.mimeType()
        );
        storytrackAttachmentRepository.save(attachment);

        String putUrl = presignedUrlProvider.presignPut(
                key,
                request.mimeType(),
                request.size(),
                PUT_EXPIRES
        );

        return new StorytrackAttachmentUploadResponse(
                attachment.getId(),
                attachment.getS3Key(),
                putUrl,
                LocalDateTime.now().plusSeconds(PUT_EXPIRES.toSeconds())
        );
    }

    @Transactional
    public void completeUpload(Long uploaderId, Long attachmentId){
        StorytrackAttachment attachment = storytrackAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_FILE_NOT_FOUND));

        // 멱등성 혹은 불필요한 처리 방지
        if(attachment.getStatus().equals(StorytrackStatus.TEMP) ||
                attachment.getStatus().equals(StorytrackStatus.DELETED)){
            return;
        }

        // 소유자 검증
        if(!attachment.getUploaderId().equals(uploaderId)){
            throw new BusinessException(ErrorCode.STORYTRACK_FILE_ATTACH_FORBIDDEN);
        }
        // 상태 검증 ( 업로딩 중인 파일만 이미지 필터링 )
        if(!attachment.getStatus().equals(StorytrackStatus.UPLOADING)){
            throw new BusinessException(ErrorCode.STORYTRACK_FILE_ATTACH_INVALID_STATUS);
        }

        // S3 업로드 검증
        StoredObjectMetadata meta;
        try {
            meta = storedObjectReader.head(attachment.getS3Key());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.STORYTRACK_FILE_UPLOAD_NOT_FINISHED);
        }

        // size 및 type 검증
        if(attachment.getFileSize() != null && attachment.getFileSize() != meta.size()){
            throw new BusinessException(ErrorCode.STORYTRACK_FILE_UPLOAD_SIZE_MISMATCH);
        }
        attachment.validateContentType(meta.contentType());

        attachment.markPending();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        StorytrackAttachmentImageModerationAsyncService
                                .moderateAsync(attachment.getId(), uploaderId);
                    }
                }
        );
    }

    @Transactional(readOnly = true)
    public StorytrackAttachmentStatusResponse getStatus(Long uploaderId, Long attachmentId){
        StorytrackAttachment attachment = storytrackAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORYTRACK_FILE_NOT_FOUND));

        // 소유자 검증
        if(!attachment.getUploaderId().equals(uploaderId)){
            throw new BusinessException(ErrorCode.STORYTRACK_FILE_ATTACH_FORBIDDEN);
        }

        return new StorytrackAttachmentStatusResponse(
                attachment.getId(),
                attachment.getStatus().name());
    }

    private String generateKey(String fileName, Long uploaderId){
        String ext = "";
        int idx = fileName.lastIndexOf(".");
        if (idx > -1) ext = fileName.substring(idx).toLowerCase();
        return "storytracks/" + uploaderId + "/" + UUID.randomUUID() + ext;
    }
}

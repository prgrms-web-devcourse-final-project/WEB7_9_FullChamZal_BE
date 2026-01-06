package back.fcz.domain.capsule.service;


import back.fcz.domain.capsule.DTO.request.CapsuleAttachmentUploadRequest;
import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentStatusResponse;
import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentUploadResponse;
import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.entity.CapsuleAttachmentStatus;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
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
public class CapsuleAttachmentPresignService {

    private final PresignedUrlProvider presignedUrlProvider;
    private final CapsuleAttachmentRepository capsuleAttachmentRepository;
    private final StoredObjectReader storedObjectReader;
    private final CapsuleImageModerationAsyncService capsuleImageModerationAsyncService;

    private static final Duration PUT_EXPIRES = Duration.ofMinutes(5);

    @Transactional
    public CapsuleAttachmentUploadResponse presignedUpload(Long uploaderId, CapsuleAttachmentUploadRequest request){
        String key = generateKey(request.filename(), uploaderId);

        CapsuleAttachment attachment = CapsuleAttachment.createUploading(
                uploaderId,
                key,
                request.filename(),
                request.size(),
                request.mimeType()
        );
        capsuleAttachmentRepository.save(attachment);

        String putUrl = presignedUrlProvider.presignPut(
                key,
                request.mimeType(),
                request.size(),
                PUT_EXPIRES
        );

        return new CapsuleAttachmentUploadResponse(
                attachment.getId(),
                attachment.getS3Key(),
                putUrl,
                LocalDateTime.now().plusSeconds(PUT_EXPIRES.toSeconds())
        );
    }

    @Transactional
    public void completeUpload(Long uploaderId, Long attachmentId){
        CapsuleAttachment attachment = capsuleAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_FILE_NOT_FOUND));

        // 멱등성 혹은 불필요한 처리 방지
        if(attachment.getStatus().equals(CapsuleAttachmentStatus.TEMP) ||
                attachment.getStatus().equals(CapsuleAttachmentStatus.DELETED)){
            return;
        }

        // 소유자 검증
        if(!attachment.getUploaderId().equals(uploaderId)){
            throw new BusinessException(ErrorCode.CAPSULE_FILE_ATTACH_FORBIDDEN);
        }
        // 상태 검증 ( 업로딩 중인 파일만 이미지 필터링 )
        if(!attachment.getStatus().equals(CapsuleAttachmentStatus.UPLOADING)){
            throw new BusinessException(ErrorCode.CAPSULE_FILE_ATTACH_INVALID_STATUS);
        }

        // S3 업로드 검증
        StoredObjectMetadata meta;
        try {
            meta = storedObjectReader.head(attachment.getS3Key());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CAPSULE_FILE_UPLOAD_NOT_FINISHED);
        }

        // size 및 type 검증
        if(attachment.getFileSize() != null && attachment.getFileSize() != meta.size()){
            throw new BusinessException(ErrorCode.CAPSULE_FILE_UPLOAD_SIZE_MISMATCH);
        }
        attachment.validateContentType(meta.contentType());


        attachment.markPending();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        capsuleImageModerationAsyncService
                                .moderateAsync(attachment.getId(), uploaderId);
                    }
                }
        );
    }

    @Transactional(readOnly = true)
    public CapsuleAttachmentStatusResponse getStatus(Long uploaderId, Long attachmentId){
        CapsuleAttachment attachment = capsuleAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_FILE_NOT_FOUND));

        // 소유자 검증
        if(!attachment.getUploaderId().equals(uploaderId)){
            throw new BusinessException(ErrorCode.CAPSULE_FILE_ATTACH_FORBIDDEN);
        }

        return new CapsuleAttachmentStatusResponse(
                attachment.getId(),
                attachment.getStatus().name());
    }
    // TODO: 캡슐 이미지 필터링 상태 한번에 확인 - 프론트에서 요청 시 구현
//    @Transactional(readOnly = true)
//    public List<AttachmentStatusResponse> getStatuses(Long uploaderId, List<Long> ids) {
//
//        List<CapsuleAttachment> list = capsuleAttachmentRepository.findAllById(ids);
//
//        return list.stream()
//                .filter(a -> a.getUploaderId().equals(uploaderId))
//                .map(a -> new AttachmentStatusResponse(
//                        a.getId(),
//                        a.getStatus().name()
//                ))
//                .toList();
//    }

    private String generateKey(String fileName, Long uploaderId){
        String ext = "";
        int idx = fileName.lastIndexOf(".");
        if (idx > -1) ext = fileName.substring(idx).toLowerCase();
        return "capsules/" + uploaderId + "/" + UUID.randomUUID() + ext;
    }
}

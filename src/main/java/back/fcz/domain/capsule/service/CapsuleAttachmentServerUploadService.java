package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentUploadResponse;
import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.service.CapsuleImageModerationService;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.infra.storage.FileStorage;
import back.fcz.infra.storage.FileUploadCommand;
import back.fcz.infra.storage.PresignedUrlProvider;
import back.fcz.infra.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CapsuleAttachmentServerUploadService {

    private final FileStorage fileStorage;
    private final CapsuleAttachmentRepository capsuleAttachmentRepository;
    private final PresignedUrlProvider presignedUrlProvider;
    private final CapsuleImageModerationService capsuleImageModerationService;

    @Transactional
    public CapsuleAttachmentUploadResponse uploadTemp (Long uploaderId, MultipartFile file){
        StoredFile stored = null;
        try {
            // 파일 유효성 검사
            if(file.isEmpty()) throw new BusinessException(ErrorCode.CAPSULE_FILE_UPLOAD_FAILED);
            if(file.getContentType() == null || !file.getContentType().startsWith("image/")) throw new BusinessException(ErrorCode.CAPSULE_FILE_UPLOAD_FAILED);


            String key = generateKey(file.getName(),uploaderId);

            // s3에 파일 업로드
            FileUploadCommand cmd = new FileUploadCommand(
                    "capsules/" + uploaderId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getInputStream(),
                    file.getSize()
            );
            stored = fileStorage.store(cmd,key);

            // 유해 이미지 필터링
            String imageUrl = presignedUrlProvider.presignGet(stored.key(), Duration.ofMinutes(5));

            capsuleImageModerationService.validateImageUrl(uploaderId, ModerationActionType.CAPSULE_CREATE,imageUrl);

            // DB에 메타데이터 저장
            CapsuleAttachment attachment = CapsuleAttachment.createUploading(
                    uploaderId,
                    stored.key(),
                    stored.filename(),
                    stored.size(),
                    stored.contentType()
            );
            capsuleAttachmentRepository.save(attachment);

            return new CapsuleAttachmentUploadResponse(
                    attachment.getId(),
                    null,
                    null,
                    null
            );
        } catch (BusinessException e) {
            if (stored != null) safeDeleteStoredFile(stored.key());
            throw e;
        } catch (Exception e) {
            if (stored != null) safeDeleteStoredFile(stored.key());
            throw new BusinessException(ErrorCode.CAPSULE_CONTENT_BLOCKED, e);
        }
    }

    private void safeDeleteStoredFile(String key) {
        try {
            fileStorage.delete(key);
        } catch (Exception ex) {
            log.warn("[S3] delete failed key={}", key, ex);
        }
    }


    private String generateKey(String fileName, Long uploaderId){
        String ext = "";
        int idx = fileName.lastIndexOf(".");
        if (idx > -1) ext = fileName.substring(idx).toLowerCase();
        return "capsules/" + uploaderId + "/" + UUID.randomUUID() + ext;
    }
}


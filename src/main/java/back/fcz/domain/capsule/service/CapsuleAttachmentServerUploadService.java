package back.fcz.domain.capsule.service;

import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentUploadResponse;
import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import back.fcz.infra.storage.FileStorage;
import back.fcz.infra.storage.FileUploadCommand;
import back.fcz.infra.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CapsuleAttachmentServerUploadService {

    private final FileStorage fileStorage;
    private final CapsuleAttachmentRepository capsuleAttachmentRepository;

    @Transactional
    public CapsuleAttachmentUploadResponse uploadTemp (Long uploaderId, MultipartFile file){
        try {
            String key = generateKey(file.getName(),uploaderId);

            // s3에 파일 업로드
            FileUploadCommand cmd = new FileUploadCommand(
                    "capsules" + uploaderId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getInputStream(),
                    file.getSize()
            );
            StoredFile stored = fileStorage.store(cmd,key);

            // DB에 메타데이터 저장
            CapsuleAttachment attachment = CapsuleAttachment.createTemp(
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
        } catch (Exception e){
            log.error("[upload] failed uploaderId={}, filename={}, size={}, ct={}",
                    uploaderId, file.getOriginalFilename(), file.getSize(), file.getContentType(), e);
            throw new BusinessException(ErrorCode.CAPSULE_FILE_UPLOAD_FAILED, e);
        }
    }

    private String generateKey(String fileName, Long uploaderId){
        String ext = "";
        int idx = fileName.lastIndexOf(".");
        if (idx > -1) ext = fileName.substring(idx).toLowerCase();
        return "capsules/" + uploaderId + "/" + UUID.randomUUID() + ext;
    }
}


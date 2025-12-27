package back.fcz.domain.capsule.service;


import back.fcz.domain.capsule.DTO.request.CapsuleAttachmentUploadRequest;
import back.fcz.domain.capsule.DTO.response.CapsuleAttachmentUploadResponse;
import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.repository.CapsuleAttachmentRepository;
import back.fcz.infra.storage.PresignedUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class CapsuleAttachmentPresignService {

    private final PresignedUrlProvider presignedUrlProvider;
    private final CapsuleAttachmentRepository capsuleAttachmentRepository;

    private static final Duration PUT_EXPIRES = Duration.ofMinutes(15); // 이미지 업로드용 presign URL 만료 시간

    public CapsuleAttachmentUploadResponse presignedUpload(Long uploaderId, CapsuleAttachmentUploadRequest request){
        String key = generateKey(request.filename(), uploaderId);

        CapsuleAttachment attachment = CapsuleAttachment.createTemp(
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

    private String generateKey(String fileName, Long uploaderId){
        String ext = "";
        int idx = fileName.lastIndexOf(".");
        if (idx > -1) ext = fileName.substring(idx).toLowerCase();
        return "capsules/temp/" + uploaderId + "/" + UUID.randomUUID() + ext;
    }
}

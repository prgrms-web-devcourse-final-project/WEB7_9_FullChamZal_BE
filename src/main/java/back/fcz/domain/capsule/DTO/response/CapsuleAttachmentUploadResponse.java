package back.fcz.domain.capsule.DTO.response;

import java.time.LocalDateTime;

public record CapsuleAttachmentUploadResponse(
        Long attachmentId,
        String s3Key,
        String presignedUrl,
        LocalDateTime expireAt
) {
}

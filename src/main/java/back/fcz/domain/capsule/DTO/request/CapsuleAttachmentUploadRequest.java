package back.fcz.domain.capsule.DTO.request;

import jakarta.validation.constraints.NotBlank;

public record CapsuleAttachmentUploadRequest(
        @NotBlank String filename,
        String mimeType,
        long size
) {
}

package back.fcz.domain.storytrack.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StorytrackAttachmentUploadRequest(
        @NotBlank String filename,
        String mimeType,
        long size
) {
}

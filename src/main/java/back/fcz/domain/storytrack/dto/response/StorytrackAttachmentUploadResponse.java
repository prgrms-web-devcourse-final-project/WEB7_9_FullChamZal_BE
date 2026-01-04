package back.fcz.domain.storytrack.dto.response;

import java.time.LocalDateTime;

public record StorytrackAttachmentUploadResponse(
        Long attachmentId,
        String s3Key,
        String presignedUrl,
        LocalDateTime expireAt
) {
}

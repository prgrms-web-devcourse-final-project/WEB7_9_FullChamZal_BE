package back.fcz.domain.storytrack.dto.response;

public record StorytrackAttachmentViewResponse(
        String presignedUrl,
        Long attachmentId
) {
}

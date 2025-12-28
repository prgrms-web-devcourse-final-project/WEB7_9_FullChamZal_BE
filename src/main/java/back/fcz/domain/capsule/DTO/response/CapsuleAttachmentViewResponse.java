package back.fcz.domain.capsule.DTO.response;

public record CapsuleAttachmentViewResponse(
        String presignedUrl,
        Long attachmentId
) {
}

package back.fcz.domain.storytrack.dto.request;

public record UpdatePathRequest(
        int stepOrderId,
        Long updatedCapsuleId
){
}
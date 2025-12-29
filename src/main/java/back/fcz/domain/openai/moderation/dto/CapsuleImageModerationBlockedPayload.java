package back.fcz.domain.openai.moderation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapsuleImageModerationBlockedPayload {
    private Long auditId;
    private String imageUrl;
    private List<ModerationViolation> violations;
}

package back.fcz.domain.openai.moderation;

import back.fcz.domain.openai.moderation.dto.OpenAiModerationResponse;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CapsuleModerationService {

    private final OpenAiModerationClient moderationClient;

    /**
     * 캡슐 저장 직전에 호출
     * - 유해 판정(flagged)이면 저장 차단
     */
    public void validateCapsuleText(
            String title,
            String content,
            String receiverNickname,
            String locationName,
            String address
    ) {
        String merged = buildInput(title, content, receiverNickname, locationName, address);

        OpenAiModerationResponse res = moderationClient.moderateText(merged).orElse(null);
        if (res == null) return; // fail-open인 경우만 여기 도달

        if (res.anyFlagged()) {
            throw new BusinessException(ErrorCode.CAPSULE_CONTENT_BLOCKED);
        }
    }

    private String buildInput(String title, String content, String receiverNickname, String locationName, String address) {
        // null-safe + 적당히 라벨 붙여서 검사(나중에 로그/디버깅에도 도움)
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) sb.append("[TITLE]\n").append(title).append("\n\n");
        if (content != null && !content.isBlank()) sb.append("[CONTENT]\n").append(content).append("\n\n");
        if (receiverNickname != null && !receiverNickname.isBlank()) sb.append("[RECEIVER]\n").append(receiverNickname).append("\n\n");
        if (locationName != null && !locationName.isBlank()) sb.append("[LOCATION_NAME]\n").append(locationName).append("\n\n");
        if (address != null && !address.isBlank()) sb.append("[ADDRESS]\n").append(address).append("\n\n");
        return sb.toString().trim();
    }
}

package back.fcz.domain.storytrack.dto.response;

import java.time.LocalDateTime;

public record JoinStorytrackResponse(
        String title, // 스토리트랙 제목
        String description,
        String storytrackType,
        int price,
        int totalSteps,
        String nickname,
        int compltedSteps,
        int lastCompletedStep,
        LocalDateTime statedAt
){
}

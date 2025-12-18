package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.storytrack.dto.PathResponse;

import java.time.LocalDateTime;
import java.util.List;

public record StorytrackDashboardResponse(
        Long storytrackId,
        String title,
        String storytrackType,
        int isPublic,
        int totalSteps,
        LocalDateTime createdAt,
        int totalParticipant,
        int completeParticipant,
        List<PathResponse> paths
){
}

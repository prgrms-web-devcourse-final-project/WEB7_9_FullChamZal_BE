package back.fcz.domain.storytrack.dto.request;

import java.util.List;

public record CreateStorytrackRequest(
        String title,
        String description,
        String trackType,
        int isPublic,
        int price,
        List<Long> capsuleList,
        Long attachmentId
){}

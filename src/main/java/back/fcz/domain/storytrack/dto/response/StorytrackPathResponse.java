package back.fcz.domain.storytrack.dto.response;

import back.fcz.domain.storytrack.dto.PathResponse;

import java.util.List;

public record StorytrackPathResponse(
        Long storytrackId,
        String title,
        String description,
        int totalStep,
        List<PathResponse> paths
) {}

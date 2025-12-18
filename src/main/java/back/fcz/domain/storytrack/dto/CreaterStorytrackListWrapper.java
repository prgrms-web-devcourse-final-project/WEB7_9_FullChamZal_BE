package back.fcz.domain.storytrack.dto;

import back.fcz.domain.storytrack.dto.response.CreaterStorytrackListResponse;

import java.util.List;

record CreaterStorytrackListWrapper(
        List<CreaterStorytrackListResponse> storytracks
) {}

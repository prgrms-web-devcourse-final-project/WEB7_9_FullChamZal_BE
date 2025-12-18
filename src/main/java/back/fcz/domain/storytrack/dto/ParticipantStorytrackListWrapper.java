package back.fcz.domain.storytrack.dto;

import back.fcz.domain.storytrack.dto.response.ParticipantStorytrackListResponse;

import java.util.List;

record ParticipantStorytrackListWrapper(
        List<ParticipantStorytrackListResponse> storytracks
) {}

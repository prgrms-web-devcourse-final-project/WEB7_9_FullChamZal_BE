package back.fcz.domain.storytrack.dto.request;


import java.util.List;

record CreateStorytrackRequest(
        String title,
        String description,
        String trackType,
        int isPublic,
        int price,
        int totalSteps,
        List<Integer> capsuleList
){
}

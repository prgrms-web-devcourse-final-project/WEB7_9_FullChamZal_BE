package back.fcz.domain.storytrack.dto.request;

import back.fcz.domain.storytrack.entity.Storytrack;

import java.util.List;

public record CreateStorytrackRequest(
        String title,
        String description,
        String trackType,
        int isPublic,
        int price,
        List<Long> capsuleList
){
    public Storytrack toEntity() {
        return Storytrack.builder()
                .title(title)
                .description(description)
                .trackType(trackType)
                .isPublic(isPublic)
                .price(price)
                .build();
    }
}

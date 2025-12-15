package back.fcz.domain.bookmark.dto;

import lombok.Builder;
import java.time.LocalDateTime;


@Builder
public record BookmarkWithCapsule(
        Long bookmarkId,
        Long capsuleId,
        String visibility,
        String sender,
        String title,
        String content,
        LocalDateTime bookmarkedAt
) {
}
package back.fcz.domain.bookmark.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 북마크 목록 조회용 DTO
 * 대시보드에서 북마크한 캡슐들의 요약 정보를 보여주기 위한 DTO
 */
@Builder
public record BookmarkListItemResponse(
        Long bookmarkId,
        Long capsuleId,
        String visibility,
        String sender,
        String title,
        String contentPreview,
        boolean isViewed,
        LocalDateTime bookmarkedAt
) {

    // 본문 미리보기
    public static String createPreview(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= 50) {
            return content;
        }
        return content.substring(0, 50) + "...";
    }
}
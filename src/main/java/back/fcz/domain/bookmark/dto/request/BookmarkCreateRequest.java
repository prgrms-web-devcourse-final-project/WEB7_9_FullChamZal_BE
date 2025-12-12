package back.fcz.domain.bookmark.dto.request;

import jakarta.validation.constraints.NotNull;

public record BookmarkCreateRequest(
        @NotNull(message = "캡슐 ID는 필수입니다")
        Long capsuleId
) {
}

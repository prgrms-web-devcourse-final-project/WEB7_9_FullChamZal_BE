package back.fcz.domain.bookmark.controller;

import back.fcz.domain.bookmark.dto.request.BookmarkCreateRequest;
import back.fcz.domain.bookmark.dto.response.BookmarkListItemResponse;
import back.fcz.domain.bookmark.service.BookmarkService;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@Tag(
        name = "북마크 API",
        description = "개인/공개 캡슐 북마크 등록/재등록/해제 API입니다."
)
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final CurrentUserContext currentUserContext;

    // 북마크 생성
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> bookmark(
            @Valid @RequestBody BookmarkCreateRequest bookmarkCreateRequest
            ) {
        Long memberId = currentUserContext.getCurrentMemberId();
        bookmarkService.createOrRestoreBookmark(memberId, bookmarkCreateRequest.capsuleId());

        return ResponseEntity.ok(ApiResponse.success());
    }

    // 북마크 해제
    @DeleteMapping("/{capsuleId}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(
            @PathVariable Long capsuleId
    ) {
        Long memberId = currentUserContext.getCurrentMemberId();
        bookmarkService.deleteBookmark(memberId, capsuleId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    // 북마크 조회
    @GetMapping
    public ResponseEntity<PageResponse<BookmarkListItemResponse>> getBookmarks(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Long memberId = currentUserContext.getCurrentMemberId();
        Page<BookmarkListItemResponse> bookmarkPage =
                bookmarkService.getBookmarks(memberId, pageable);

        return ResponseEntity.ok(new PageResponse<>(bookmarkPage));
    }
}

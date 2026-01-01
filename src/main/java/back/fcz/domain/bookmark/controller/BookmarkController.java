package back.fcz.domain.bookmark.controller;

import back.fcz.domain.bookmark.dto.request.BookmarkCreateRequest;
import back.fcz.domain.bookmark.dto.response.BookmarkListItemResponse;
import back.fcz.domain.bookmark.service.BookmarkService;
import back.fcz.domain.member.service.CurrentUserContext;
import back.fcz.global.config.swagger.ApiErrorCodeExample;
import back.fcz.global.dto.PageResponse;
import back.fcz.global.exception.ErrorCode;
import back.fcz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @PostMapping
    @Operation(summary = "북마크 생성/복구", description = "열람한 캡슐을 북마크에 추가하거나 삭제된 북마크를 복구하는 API입니다.")
    @ApiErrorCodeExample({
            ErrorCode.CAPSULE_NOT_FOUND,
            ErrorCode.INVALID_CAPSULE_VISIBILITY,
            ErrorCode.BOOKMARK_ALREADY_EXISTS,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_EXPIRED,
            ErrorCode.TOKEN_INVALID
    })
    public ResponseEntity<ApiResponse<Void>> bookmark(
            @Valid @RequestBody BookmarkCreateRequest bookmarkCreateRequest
            ) {
        Long memberId = currentUserContext.getCurrentMemberId();
        bookmarkService.createOrRestoreBookmark(memberId, bookmarkCreateRequest.capsuleId());

        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{capsuleId}")
    @Operation(summary = "북마크 해제", description = "북마크된 캡슐을 북마크 목록에서 제거하는 API입니다.")
    @ApiErrorCodeExample({
            ErrorCode.BOOKMARK_NOT_FOUND,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_EXPIRED,
            ErrorCode.TOKEN_INVALID
    })
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(
            @PathVariable Long capsuleId
    ) {
        Long memberId = currentUserContext.getCurrentMemberId();
        bookmarkService.deleteBookmark(memberId, capsuleId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping
    @Operation(summary = "북마크 목록 조회", description = "북마크된 캡슐 목록을 페이징하여 조회하는 API입니다.")
    @ApiErrorCodeExample({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.MEMBER_NOT_ACTIVE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.TOKEN_EXPIRED,
            ErrorCode.TOKEN_INVALID
    })
    public ResponseEntity<PageResponse<BookmarkListItemResponse>> getBookmarks(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long memberId = currentUserContext.getCurrentMemberId();

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<BookmarkListItemResponse> bookmarkPage =
                bookmarkService.getBookmarks(memberId, sortedPageable);

        return ResponseEntity.ok(new PageResponse<>(bookmarkPage));
    }
}

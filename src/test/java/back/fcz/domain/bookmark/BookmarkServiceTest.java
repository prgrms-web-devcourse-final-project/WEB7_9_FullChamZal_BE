package back.fcz.domain.bookmark;

import back.fcz.domain.bookmark.dto.BookmarkWithCapsule;
import back.fcz.domain.bookmark.dto.response.BookmarkListItemResponse;
import back.fcz.domain.bookmark.entity.Bookmark;
import back.fcz.domain.bookmark.repository.BookmarkRepository;
import back.fcz.domain.bookmark.service.BookmarkService;
import back.fcz.domain.capsule.entity.Capsule;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @InjectMocks
    private BookmarkService bookmarkService;

    @Mock
    private CapsuleRepository capsuleRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private Capsule capsule;

    @Test
    @DisplayName("개인 캡슐 북마크 생성 - 성공")
    void createBookmark_PrivateCapsule_Success() {
        Long memberId = 1L;
        Long capsuleId = 100L;

        given(capsuleRepository.findById(capsuleId)).willReturn(Optional.of(capsule));
        given(bookmarkRepository.findByMemberIdAndCapsuleId(memberId, capsuleId))
                .willReturn(Optional.empty());

        bookmarkService.createOrRestoreBookmark(memberId, capsuleId);

        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("공개 캡슐 북마크 생성 - 성공")
    void createBookmark_PublicCapsule_Success() {
        Long memberId = 1L;
        Long capsuleId = 100L;

        given(capsuleRepository.findById(capsuleId)).willReturn(Optional.of(capsule));
        given(bookmarkRepository.findByMemberIdAndCapsuleId(memberId, capsuleId))
                .willReturn(Optional.empty());

        bookmarkService.createOrRestoreBookmark(memberId, capsuleId);

        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("북마크 생성 - 이미 북마크됨")
    void createBookmark_AlreadyExists() {
        Long memberId = 1L;
        Long capsuleId = 100L;

        Bookmark bookmark = mock(Bookmark.class);
        given(bookmark.getDeletedAt()).willReturn(null);

        given(capsuleRepository.findById(capsuleId)).willReturn(Optional.of(capsule));
        given(bookmarkRepository.findByMemberIdAndCapsuleId(memberId, capsuleId))
                .willReturn(Optional.of(bookmark));

        assertThatThrownBy(() -> bookmarkService.createOrRestoreBookmark(memberId, capsuleId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKMARK_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("북마크 복구 - 성공")
    void restoreBookmark_Success() {
        Long memberId = 1L;
        Long capsuleId = 100L;

        Bookmark deletedBookmark = mock(Bookmark.class);
        given(deletedBookmark.getDeletedAt()).willReturn(LocalDateTime.now());

        given(capsuleRepository.findById(capsuleId)).willReturn(Optional.of(capsule));
        given(bookmarkRepository.findByMemberIdAndCapsuleId(memberId, capsuleId))
                .willReturn(Optional.of(deletedBookmark));

        bookmarkService.createOrRestoreBookmark(memberId, capsuleId);

        verify(deletedBookmark).restore();
    }

    @Test
    @DisplayName("북마크 삭제 - 성공")
    void deleteBookmark_Success() {
        Long memberId = 1L;
        Long capsuleId = 100L;

        Bookmark bookmark = mock(Bookmark.class);

        given(bookmarkRepository.findByMemberIdAndCapsuleIdAndDeletedAtIsNull(memberId, capsuleId))
                .willReturn(Optional.of(bookmark));

        bookmarkService.deleteBookmark(memberId, capsuleId);

        verify(bookmark).markDeleted();
    }

    @Test
    @DisplayName("북마크 목록 조회 - 성공")
    void getBookmarks_Success() {
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        BookmarkWithCapsule dto = new BookmarkWithCapsule(
                1L, 100L, "PRIVATE", "sender", "title", "content", LocalDateTime.now()
        );

        given(bookmarkRepository.findBookmarksWithCapsuleInfo(memberId, pageable))
                .willReturn(new PageImpl<>(List.of(dto)));

        Page<BookmarkListItemResponse> result =
                bookmarkService.getBookmarks(memberId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isViewed()).isTrue();
    }
}

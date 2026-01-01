package back.fcz.domain.bookmark.service;

import back.fcz.domain.bookmark.dto.BookmarkWithCapsule;
import back.fcz.domain.bookmark.dto.response.BookmarkListItemResponse;
import back.fcz.domain.bookmark.entity.Bookmark;
import back.fcz.domain.bookmark.repository.BookmarkRepository;
import back.fcz.domain.capsule.repository.CapsuleRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final CapsuleRepository capsuleRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public void createOrRestoreBookmark(Long memberId, Long capsuleId) {
        capsuleRepository.findById(capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAPSULE_NOT_FOUND));

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByMemberIdAndCapsuleId(memberId, capsuleId);

        if (existingBookmark.isEmpty()) {
            Bookmark newBookmark = Bookmark.builder()
                    .memberId(memberId)
                    .capsuleId(capsuleId)
                    .build();

            bookmarkRepository.save(newBookmark);
            log.info("북마크 생성 완료 - memberId: {}, capsuleId: {}", memberId, capsuleId);
            return;
        }

        Bookmark bookmark = existingBookmark.get();

        if (bookmark.getDeletedAt() == null) {
            log.warn("이미 북마크된 캡슐 - memberId: {}, capsuleId: {}", memberId, capsuleId);
            throw new BusinessException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        bookmark.restore();
        log.info("북마크 재생성 완료 - memberId: {}, capsuleId: {}", memberId, capsuleId);
    }


    @Transactional
    public void deleteBookmark(Long memberId, Long capsuleId) {
        Bookmark bookmark = bookmarkRepository.findByMemberIdAndCapsuleIdAndDeletedAtIsNull(memberId, capsuleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKMARK_NOT_FOUND));

        bookmark.markDeleted();
        log.info("북마크 해제 완료 - memberId: {}, capsuleId: {}", memberId, capsuleId);
    }


    public Page<BookmarkListItemResponse> getBookmarks(Long memberId, Pageable pageable) {
        Page<BookmarkWithCapsule> bookmarkWithCapsulePage = bookmarkRepository.findBookmarksWithCapsuleInfo(memberId, pageable);

        return bookmarkWithCapsulePage.map(dto ->
                BookmarkListItemResponse.builder()
                        .bookmarkId(dto.bookmarkId())
                        .capsuleId(dto.capsuleId())
                        .visibility(dto.visibility())
                        .sender(dto.sender())
                        .title(dto.title())
                        .contentPreview(BookmarkListItemResponse.createPreview(dto.content()))
                        .isViewed(true) // 북마크 생성 시점에 이미 열람 완료 검증됨
                        .bookmarkedAt(dto.bookmarkedAt())
                        .build()
        );
    }
}

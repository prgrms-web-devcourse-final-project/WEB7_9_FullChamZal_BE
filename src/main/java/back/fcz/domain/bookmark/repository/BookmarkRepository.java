package back.fcz.domain.bookmark.repository;

import back.fcz.domain.bookmark.dto.BookmarkWithCapsule;
import back.fcz.domain.bookmark.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByMemberIdAndCapsuleId(Long memberId, Long capsuleId);

    Optional<Bookmark> findByMemberIdAndCapsuleIdAndDeletedAtIsNull(Long memberId, Long capsuleId);

    @Query("""
    SELECT new back.fcz.domain.bookmark.dto.BookmarkWithCapsule(
        b.id,
        b.capsuleId,
        c.visibility,
        c.nickname,
        c.title,
        c.content,
        b.createdAt
    )
    FROM Bookmark b
    JOIN Capsule c ON b.capsuleId = c.capsuleId
    WHERE b.memberId = :memberId
    AND b.deletedAt IS NULL
    AND c.isDeleted = 0
    """)
    Page<BookmarkWithCapsule> findBookmarksWithCapsuleInfo(@Param("memberId") Long memberId, Pageable pageable);}

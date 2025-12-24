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

    boolean existsByMemberIdAndCapsuleIdAndDeletedAtIsNull(Long memberId, Long capsuleId);
    
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
    FROM Bookmark b, Capsule c
    WHERE b.capsuleId = c.capsuleId
    AND b.memberId = :memberId
    AND b.deletedAt IS NULL
    AND c.isDeleted = 0
    ORDER BY b.createdAt DESC
    """)
    Page<BookmarkWithCapsule> findBookmarksWithCapsuleInfo(@Param("memberId") Long memberId, Pageable pageable);
}

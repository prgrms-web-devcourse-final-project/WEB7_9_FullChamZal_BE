package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.Storytrack;
import back.fcz.domain.storytrack.entity.StorytrackAttachment;
import back.fcz.domain.storytrack.entity.StorytrackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StorytrackAttachmentRepository extends JpaRepository<StorytrackAttachment, Long> {

    // 스토리트랙 다건 조회 시 사용
    @Query("""
SELECT a
FROM StorytrackAttachment a
WHERE a.storytrack.storytrackId IN :storytrackIds
  AND a.status = 'THUMBNAIL'
  AND a.deletedAt IS NULL
""")
    List<StorytrackAttachment> findActiveImagesByStorytrackIds(List<Long> storytrackIds);

    List<StorytrackAttachment> findTop1000ByStatusAndExpiredAtBeforeOrderByIdAsc(
            StorytrackStatus status,
            LocalDateTime time
    );

    Optional<StorytrackAttachment> findByIdAndUploaderIdAndStatusAndDeletedAtIsNull(Long attachmentId, Long memberId, StorytrackStatus storytrackStatus);

    Optional<StorytrackAttachment> findByStorytrackAndStatusAndDeletedAtIsNull(Storytrack storytrack, StorytrackStatus storytrackStatus);

    List<StorytrackAttachment> findByStorytrack_StorytrackIdAndStatus(Long storytrackId, StorytrackStatus storytrackStatus);

    Optional<StorytrackAttachment> findByStorytrack_StorytrackIdAndStatusAndDeletedAtIsNull(Long storytrackId, StorytrackStatus storytrackStatus);

    List<StorytrackAttachment> findTop1000ByStatusAndCreatedAtBeforeOrderByIdAsc(StorytrackStatus storytrackStatus, LocalDateTime localDateTime);

    List<StorytrackAttachment> findTop1000ByStatusOrderByDeletedAtAsc(StorytrackStatus storytrackStatus);
}

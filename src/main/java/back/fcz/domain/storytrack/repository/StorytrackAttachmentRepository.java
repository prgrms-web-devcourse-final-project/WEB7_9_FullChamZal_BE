package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.StorytrackAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StorytrackAttachmentRepository extends JpaRepository<StorytrackAttachment, Long> {
    // 스토리트랙 단일 조회시 사용
    List<StorytrackAttachment> findByStorytrack_StorytrackIdAndDeletedAtIsNull(Long storytrackId);


    // 스토리트랙 다건 조회 시 사용
    @Query("""
SELECT a
FROM StorytrackAttachment a
WHERE a.storytrack.storytrackId IN :storytrackIds
  AND a.deletedAt IS NULL
""")
    List<StorytrackAttachment> findActiveImagesByStorytrackIds(List<Long> storytrackIds);
}

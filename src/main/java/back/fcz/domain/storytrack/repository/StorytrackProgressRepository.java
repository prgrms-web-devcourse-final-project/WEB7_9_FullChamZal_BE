package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.StorytrackProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StorytrackProgressRepository extends JpaRepository<StorytrackProgress, Long> {
    Optional<StorytrackProgress> findByMember_MemberIdAndStorytrack_StorytrackId(Long memberId, Long storytrackId);

    @Query("""
    SELECT COUNT(sp)
    FROM StorytrackProgress sp
    WHERE sp.storytrack.storytrackId = :storytrackId
      AND sp.deletedAt IS NULL
""")
    long countActiveParticipants(@Param("storytrackId") Long storytrackId);

}

package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.StorytrackStep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorytrackStepRepository extends JpaRepository<StorytrackStep, Long> {
    List<StorytrackStep> findAllByStorytrack_StorytrackId(Long storytrackId);

    Optional<Object> findByStorytrack_StorytrackIdAndStepOrder(Long storytrackId, int stpeOrderId);

    @Query(
            value = """
        select s
        from StorytrackStep s
        join fetch s.capsule c
        join fetch c.unlock u
        where s.storytrack.storytrackId = :storytrackId
        """,
            countQuery = """
        select count(s)
        from StorytrackStep s
        where s.storytrack.storytrackId = :storytrackId
        """
    )
    Page<StorytrackStep> findStepsWithCapsule(
            @Param("storytrackId") Long storytrackId,
            Pageable pageable
    );

}

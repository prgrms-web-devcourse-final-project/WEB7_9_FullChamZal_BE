package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.StorytrackStep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorytrackStepRepository extends JpaRepository<StorytrackStep, Long> {
    List<StorytrackStep> findAllByStorytrack_StorytrackId(Long storytrackId);

    Optional<Object> findByStorytrack_StorytrackIdAndStepOrder(Long storytrackId, int stpeOrderId);

    @Query("""
    select s
    from StorytrackStep s
    join fetch s.capsule c
    where s.storytrack.storytrackId = :storytrackId
    order by s.stepOrder asc
""")
    Page<StorytrackStep> findStepsWithCapsule(
            @Param("storytrackId") Long storytrackId,
            Pageable pagable
    );

    Optional<StorytrackStep> findByCapsule_CapsuleIdAndStorytrack_StorytrackId(Long capsuleId, Long storytrackId);

    @Query("""
        select case when count(s) > 0 then true else false end
        from StorytrackStep s
        where s.storytrack.storytrackId = :storytrackId
          and s.stepOrder = :stepOrder
    """)
    boolean existsByStorytrackIdAndStepOrder(@Param("storytrackId") Long storytrackId,
                                             @Param("stepOrder") int stepOrder);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StorytrackStep ss where ss.capsule.capsuleId in :capsuleIds")
    int deleteByCapsuleIds(@Param("capsuleIds") List<Long> capsuleIds);
}

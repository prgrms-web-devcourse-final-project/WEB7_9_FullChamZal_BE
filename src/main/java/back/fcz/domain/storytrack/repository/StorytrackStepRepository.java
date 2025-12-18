package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.StorytrackStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorytrackStepRepository extends JpaRepository<StorytrackStep, Long> {
    List<StorytrackStep> findAllByStorytrack_StorytrackId(Long storytrackId);

    Optional<Object> findByStorytrackIdAndStepOrderId(Long aLong, int i);
}

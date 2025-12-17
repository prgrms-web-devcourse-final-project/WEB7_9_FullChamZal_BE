package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.StorytrackStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorytrackStepRepository extends JpaRepository<StorytrackStep, Long> {
    List<StorytrackStep> findAllByStorytrack_StorytrackId(Long storytrackId);
}

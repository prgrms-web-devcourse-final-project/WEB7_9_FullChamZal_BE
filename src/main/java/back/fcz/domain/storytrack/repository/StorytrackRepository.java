package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.Storytrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorytrackRepository extends JpaRepository<Storytrack, Long> {
}

package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.StorytrackProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StorytrackProgressRepository extends JpaRepository<StorytrackProgress, Long> {
    Optional<StorytrackProgress> findByMember_MemberIdAndStorytrack_StorytrackId(Long memberId, Long storytrackId);

    Long countMemberIdByStorytrack_StorytrackId(Long storytrackId);
}

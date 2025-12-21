package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.Storytrack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorytrackRepository extends JpaRepository<Storytrack, Long> {

    Page<Storytrack> findByIsPublic(int isPublic, Pageable pagealble);

    Optional<Storytrack> findByStorytrackIdAndIsDeleted(Long id, int isDeleted);

    List<Storytrack> findByMember_MemberId(Long memberId);
}

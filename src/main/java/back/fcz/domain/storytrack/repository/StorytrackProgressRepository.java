package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.entity.StorytrackProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorytrackProgressRepository extends JpaRepository<StorytrackProgress, Long> {
    Optional<StorytrackProgress> findByMember_MemberIdAndStorytrack_StorytrackId(Long memberId, Long storytrackId);

    // 스토리트랙 참여자 집계
    @Query("""
    SELECT COUNT(sp)
    FROM StorytrackProgress sp
    WHERE sp.storytrack.storytrackId = :storytrackId
      AND sp.deletedAt IS NULL
""")
    long countActiveParticipants(@Param("storytrackId") Long storytrackId);

    // 스토리트랙 참여자 조회
    StorytrackProgress findByStorytrack_StorytrackId(Long storytrackId);

    // 집계용
    int countByStorytrack_StorytrackId(Long storytrackId);
    int countByStorytrack_StorytrackIdAndCompletedAtIsNotNull(Long storytrackId);


    @Query("""
    select sp
    from StorytrackProgress sp
    join fetch sp.storytrack s
    where sp.member.memberId = :memberId
""")
    List<StorytrackProgress> findProgressesByMemberId(Long memberId);

    Optional<StorytrackProgress> findByStorytrack_StorytrackIdAndMember_MemberId(Long storytrackId, Long memberId);

    boolean existsByMember_MemberIdAndStorytrack_StorytrackId(Long memberId, Long storytrackId);
}

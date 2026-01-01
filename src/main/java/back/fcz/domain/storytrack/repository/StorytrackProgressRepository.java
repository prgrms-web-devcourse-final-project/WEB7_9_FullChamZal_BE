package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.dto.response.ParticipantStorytrackListResponse;
import back.fcz.domain.storytrack.entity.StorytrackProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StorytrackProgressRepository extends JpaRepository<StorytrackProgress, Long> {
    Optional<StorytrackProgress> findByMember_MemberIdAndStorytrack_StorytrackId(Long memberId, Long storytrackId);

    Optional<StorytrackProgress>
    findByStorytrack_StorytrackIdAndMember_MemberIdAndDeletedAtIsNull( Long storytrackId, Long memberId);

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
    and sp.deletedAt IS NULL
""")
    Page<StorytrackProgress> findProgressesByMemberId(Long memberId, Pageable pageable);

    Optional<StorytrackProgress> findByStorytrack_StorytrackIdAndMember_MemberId(Long storytrackId, Long memberId);

    boolean existsByMember_MemberIdAndStorytrack_StorytrackIdAndDeletedAt(Long memberId, Long storytrackId, LocalDateTime deleteAt);

    @Query("""
SELECT new back.fcz.domain.storytrack.dto.response.ParticipantStorytrackListResponse(
    m.memberId,
    s.storytrackId,
    m.nickname,
    s.title,
    s.description,
    s.trackType,
    s.isPublic,
    s.price,
    s.totalSteps,
    p.completedSteps,
    p.lastCompletedStep,
    p.startedAt,
    p.completedAt,
    s.createdAt,
    COUNT(sp2)
)
FROM StorytrackProgress p
JOIN p.member m
JOIN p.storytrack s
LEFT JOIN StorytrackProgress sp2
    ON sp2.storytrack = s
WHERE m.memberId = :memberId
AND p.deletedAt IS NULL
GROUP BY p, m, s
""")
    Page<ParticipantStorytrackListResponse> findJoinedStorytracksWithMemberCount(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    boolean existsByMember_MemberIdAndStorytrack_StorytrackId(Long memberId, Long storytrackId);
}

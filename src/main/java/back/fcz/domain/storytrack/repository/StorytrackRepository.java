package back.fcz.domain.storytrack.repository;

import back.fcz.domain.storytrack.dto.response.CreaterStorytrackListResponse;
import back.fcz.domain.storytrack.dto.response.TotalStorytrackResponse;
import back.fcz.domain.storytrack.entity.Storytrack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StorytrackRepository extends JpaRepository<Storytrack, Long> {

    Page<Storytrack> findByIsPublic(int isPublic, Pageable pageable);

    Optional<Storytrack> findByStorytrackIdAndIsDeleted(Long id, int isDeleted);

    Page<Storytrack> findByMember_MemberId(Long memberId, Pageable pageable);

    // 스토리트랙 목록 조회 시, 참여자 수 조회를 위해서
    @Query("""
SELECT new back.fcz.domain.storytrack.dto.response.TotalStorytrackResponse(
    s.storytrackId,
    m.nickname,
    s.title,
    s.description,
    s.trackType,
    s.isPublic,
    s.price,
    s.totalSteps,
    s.createdAt,
    COUNT(spAll),
    CASE
        WHEN m.memberId = :loginMemberId THEN back.fcz.domain.storytrack.dto.StorytrackMemberType.CREATOR
        WHEN spMe.completedAt IS NOT NULL THEN back.fcz.domain.storytrack.dto.StorytrackMemberType.COMPLETED
        WHEN spMe.id IS NOT NULL THEN back.fcz.domain.storytrack.dto.StorytrackMemberType.PARTICIPANT
        ELSE back.fcz.domain.storytrack.dto.StorytrackMemberType.NOT_JOINED
    END
)
FROM Storytrack s
JOIN s.member m
LEFT JOIN StorytrackProgress spAll
    ON spAll.storytrack = s
LEFT JOIN StorytrackProgress spMe
    ON spMe.storytrack = s
   AND spMe.member.memberId = :loginMemberId
WHERE s.isPublic = 1
  AND s.isDeleted = 0
GROUP BY s, m, spMe
""")
    Page<TotalStorytrackResponse> findPublicStorytracksWithMemberType(
            @Param("loginMemberId") Long loginMemberId,
            Pageable pageable
    );

    // 내가 생성한 스토리트랙 조회 시, 참여자 수 포함
    @Query("""
    SELECT new back.fcz.domain.storytrack.dto.response.CreaterStorytrackListResponse(
        s.storytrackId,
        s.title,
        s.description,
        s.trackType,
        s.isPublic,
        s.price,
        s.totalSteps,
        s.createdAt,
        COUNT(sp)
    )
    FROM Storytrack s
    LEFT JOIN StorytrackProgress sp
        ON sp.storytrack = s
    WHERE s.member.memberId = :memberId
      AND s.isDeleted = 0
    GROUP BY s
    """)
    Page<CreaterStorytrackListResponse> findCreatedStorytracksWithMemberCount(
            @Param("memberId") Long memberId,
            Pageable pageable
    );
}

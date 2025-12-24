package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.Capsule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CapsuleRepository extends JpaRepository<Capsule, Long> {

    // 기본: 삭제되지 않은 캡슐만 조회
    Page<Capsule> findByIsDeletedFalse(Pageable pageable);

    // visibility 필터 (PUBLIC / PRIVATE)
    Page<Capsule> findByIsDeletedFalseAndVisibility(String visibility, Pageable pageable);

    // uuid로 캡슐 찾기
    Optional<Capsule> findByUuid(String uuid);

    // capsuleId로 캡슐 찾기
    Optional<Capsule> findByCapsuleId(Long capsuleId);

    // 선착순
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Capsule c 
    SET c.currentViewCount = c.currentViewCount + 1 
    WHERE c.capsuleId = :capsuleId 
    AND (c.maxViewCount IS NULL OR c.currentViewCount < c.maxViewCount)
""")
    int incrementViewCountIfAvailable(@Param("capsuleId") Long capsuleId);
    // TODO: 작성자, 기간, 키워드 검색 등은 추후 QueryDsl / Specification 으로 확장
    // 공개 캡슐이고 삭제되지 않았으며, 위치 정보가 유효한 캡슐 조회
    @Query("SELECT c FROM Capsule c " +
            "WHERE c.visibility = :visibility " +
            "AND c.isDeleted = :isDeleted " +
            "AND c.locationLat IS NOT NULL " +
            "AND c.locationLng IS NOT NULL")
    List<Capsule> findOpenCapsule(@Param("visibility") String visibility, @Param("isDeleted") int isDeleted);

    //memberId와 isDeleted=0 조건을 만족하는 Capsule 목록 조회
    @Query("SELECT c FROM Capsule c WHERE c.memberId.memberId = :memberId AND c.isDeleted = 0")
    Page<Capsule> findActiveCapsulesByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    Optional<Capsule> findByCapsuleIdAndMemberId_MemberId(Long capsuleId, Long memberId);

    @Query("""
    select v.currentViewCount
    from Capsule v
    where v.capsuleId = :capsuleId
""")
    int findCurrentViewCountByCapsuleId(@Param("capsuleId") Long capsuleId);

    /**
     * Admin 캡슐 목록 조회(검색)
     * - visibility: PUBLIC/PRIVATE/null(전체)
     * - deleted: true(삭제된 것만: isDeleted != 0), false(미삭제만: isDeleted=0), null(전체)
     * - keyword: title/nickname/uuid 부분일치
     */
    @Query("""
        select c
        from Capsule c
        where (:visibility is null or c.visibility = :visibility)
          and (
                :deleted is null
                or (:deleted = true and c.isDeleted <> 0)
                or (:deleted = false and c.isDeleted = 0)
          )
          and (
                :keyword is null or :keyword = ''
                or lower(coalesce(c.title, '')) like lower(concat('%', :keyword, '%'))
                or lower(c.nickname) like lower(concat('%', :keyword, '%'))
                or c.uuid like concat('%', :keyword, '%')
          )
        """)
    Page<Capsule> searchAdmin(
            @Param("visibility") String visibility,
            @Param("deleted") Boolean deleted,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 관리자 캡슐 검색/필터
    @Query("""
        select c
        from Capsule c
        where (:visibility is null or :visibility = '' or c.visibility = :visibility)
          and (:isDeleted is null or c.isDeleted = :isDeleted)
          and (:isProtected is null or c.isProtected = :isProtected)
          and (
                      :unlocked is null\s
                      or (:unlocked = true and c.currentViewCount > 0)
                      or (:unlocked = false and c.currentViewCount = 0)
              )
          and (
                :keyword is null or :keyword = '' or
                lower(c.title) like lower(concat('%', :keyword, '%')) or
                lower(c.content) like lower(concat('%', :keyword, '%')) or
                lower(c.nickname) like lower(concat('%', :keyword, '%')) or
                lower(c.uuid) like lower(concat('%', :keyword, '%'))
              )
    """)
    Page<Capsule> searchAdmin(
            @Param("visibility") String visibility,
            @Param("isDeleted") Integer isDeleted,
            @Param("isProtected") Integer isProtected,
            @Param("unlocked") Boolean unlocked,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 회원 최근 캡슐 5개(미삭제)
    List<Capsule> findTop5ByMemberId_MemberIdAndIsDeletedOrderByCreatedAtDesc(Long memberId, int isDeleted);

    // 회원별 "미삭제 캡슐 수" 배치 집계
    @Query("""
        select c.memberId.memberId, count(c)
        from Capsule c
        where c.isDeleted = 0
          and c.memberId.memberId in :memberIds
        group by c.memberId.memberId
    """)
    List<Object[]> countActiveByMemberIds(@Param("memberIds") List<Long> memberIds);

    // 회원별 "보호(블라인드) 캡슐 수" 배치 집계 (보호:1)
    @Query("""
        select c.memberId.memberId, count(c)
        from Capsule c
        where c.isDeleted = 0
          and c.isProtected = :protectedValue
          and c.memberId.memberId in :memberIds
        group by c.memberId.memberId
    """)
    List<Object[]> countProtectedActiveByMemberIds(
            @Param("memberIds") List<Long> memberIds,
            @Param("protectedValue") int protectedValue
    );

    long countByMemberId_MemberIdAndIsDeleted(Long memberId, int isDeleted);
    long countByMemberId_MemberIdAndIsDeletedAndIsProtected(Long memberId, int isDeleted, int isProtected);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Capsule c SET c.likeCount = c.likeCount + 1 WHERE c.capsuleId = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Capsule c SET c.likeCount = c.likeCount - 1 WHERE c.capsuleId = :id AND c.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    // 송신 캡슐 월별 카운트
    @Query("SELECT month(c.createdAt), count(c) " +
            "FROM Capsule c " +
            "WHERE c.memberId.memberId = :memberId AND year(c.createdAt) = :year " +
            "GROUP BY month(c.createdAt)")
    List<Object[]> countMonthlySendCapsules(@Param("memberId") Long memberId, @Param("year") int year);
           
    @Query("""
    SELECT c FROM Capsule c
    WHERE c.memberId.memberId = :memberId
      AND c.visibility = :isPublic
      AND (c.unlockType = :type1 OR c.unlockType = :type2)
""")
    Page<Capsule> findMyCapsulesLocationType(
            @Param("memberId") Long memberId,
            @Param("isPublic") String isPublic,
            @Param("type1") String type1,
            @Param("type2") String type2,
            Pageable pageable
    );
}

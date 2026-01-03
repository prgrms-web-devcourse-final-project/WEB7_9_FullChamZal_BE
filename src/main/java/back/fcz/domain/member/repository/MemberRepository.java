package back.fcz.domain.member.repository;

import back.fcz.domain.admin.member.dto.AdminMemberStatistics;
import back.fcz.domain.member.entity.Member;
import back.fcz.domain.member.entity.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByUserId(String userId);
    boolean existsByNickname(String nickname);
    boolean existsByPhoneHash(String phoneHash);

    Optional<Member> findByUserId(String userId);
    Optional<Member> findByPhoneHash(String hashedPhone);

    boolean existsByUserIdAndDeletedAtIsNull(String userId);
    Optional<Member> findByUserIdAndDeletedAtIsNotNull(String userId);
    boolean existsByPhoneHashAndMemberIdNot(String phoneHash, Long memberId);

    boolean existsByPhoneHashAndDeletedAtIsNull(String phoneHash);
    Optional<Member> findByPhoneHashAndDeletedAtIsNotNull(String phoneHash);

    List<Member> findAllByStatusAndDeletedAtBefore(MemberStatus status, LocalDateTime deletedAt);

    // ✅ Admin 검색
    @Query("""
        select m
        from Member m
        where (:status is null or m.status = :status)
          and (
                :keyword is null or :keyword = '' or
                lower(m.userId) like lower(concat('%', :keyword, '%')) or
                lower(m.nickname) like lower(concat('%', :keyword, '%')) or
                lower(m.name) like lower(concat('%', :keyword, '%')) or
                lower(m.phoneHash) like lower(concat('%', :keyword, '%'))
              )
          and (:from is null or m.createdAt >= :from)
          and (:to is null or m.createdAt < :to)
    """)
    Page<Member> searchAdmin(
            @Param("status") MemberStatus status,
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    /**
     * 회원 통계 정보 통합 조회 (성능 개선)
     * - 캡슐 수, 보호 캡슐 수, 신고당한 횟수를 한 번의 쿼리로 조회
     * - LEFT JOIN을 사용하여 캡슐이 없는 회원도 포함
     */
    @Query("""
        select new back.fcz.domain.admin.member.dto.AdminMemberStatistics(
            m.memberId,
            coalesce(cast(sum(case when c.isDeleted = 0 then 1 else 0 end) as long), 0L),
            coalesce(cast(sum(case when c.isDeleted = 0 and c.isProtected = :protectedValue then 1 else 0 end) as long), 0L),
            coalesce(cast(count(distinct r.id) as long), 0L)
        )
        from Member m
        left join Capsule c on c.memberId.memberId = m.memberId
        left join Report r on r.capsule.memberId.memberId = m.memberId
        where m.memberId in :memberIds
        group by m.memberId
    """)
    List<AdminMemberStatistics> getMemberStatisticsBatch(
            @Param("memberIds") List<Long> memberIds,
            @Param("protectedValue") int protectedValue
    );
}

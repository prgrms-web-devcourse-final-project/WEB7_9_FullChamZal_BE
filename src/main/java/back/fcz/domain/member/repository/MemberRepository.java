package back.fcz.domain.member.repository;

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

    // userId 관련
    boolean existsByUserIdAndDeletedAtIsNull(String userId);
    Optional<Member> findByUserIdAndDeletedAtIsNotNull(String userId);
    boolean existsByPhoneHashAndMemberIdNot(String phoneHash, Long memberId);

    // phoneHash 관련
    boolean existsByPhoneHashAndDeletedAtIsNull(String phoneHash);
    Optional<Member> findByPhoneHashAndDeletedAtIsNotNull(String phoneHash);

    List<Member> findAllByStatusAndDeletedAtBefore(MemberStatus status, LocalDateTime deletedAt);

    // ===== Admin 검색 =====
    @Query("""
        select m
        from Member m
        where (:status is null or m.status = :status)
          and (
               :keyword is null or :keyword = ''
               or lower(m.userId) like lower(concat('%', :keyword, '%'))
               or lower(m.nickname) like lower(concat('%', :keyword, '%'))
               or m.phoneHash like concat('%', :keyword, '%')
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
}

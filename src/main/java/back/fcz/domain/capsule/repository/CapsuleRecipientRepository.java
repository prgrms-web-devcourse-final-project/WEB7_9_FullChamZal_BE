package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.CapsuleRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CapsuleRecipientRepository extends JpaRepository<CapsuleRecipient,Long> {

    Optional<CapsuleRecipient> findByCapsuleId_CapsuleId(Long capsuleId);

    boolean existsByCapsuleId_CapsuleIdAndRecipientPhoneHash(Long capsuleId, String recipientPhoneHash);

    boolean existsByCapsuleId_CapsuleId(Long capsuleId);

    // 회원 탈퇴 시
    List<CapsuleRecipient> findAllByRecipientPhoneHash(String recipientPhoneHash);

    @Modifying
    @Query("""
        UPDATE CapsuleRecipient pcr
        SET pcr.recipientName = '탈퇴한 수신자',
            pcr.recipientPhone = CONCAT('DELETED_', pcr.id),
            pcr.recipientPhoneHash = CONCAT('DELETED_', pcr.id)
        WHERE pcr.recipientPhoneHash = :phoneHash
    """)
    void anonymizeByRecipientPhoneHash(@Param("phoneHash") String phoneHash,
                                       @Param("memberId") Long memberId);

    Optional<CapsuleRecipient> findByCapsuleId_CapsuleIdAndRecipientPhoneHash(Long capsuleId, String phoneHash);
    // phoneHash를 가지는 수신자 리스트 조회. JOIN을 이용하여 Capsule 테이블도 같이 조회
    @Query("SELECT cr FROM CapsuleRecipient cr JOIN FETCH cr.capsuleId c WHERE cr.recipientPhoneHash = :phoneHash AND cr.deletedAt IS NULL")
    Page<CapsuleRecipient> findAllByRecipientPhoneHashWithCapsule(@Param("phoneHash") String phoneHash, Pageable pageable);

    @Query("""
        select cr
        from CapsuleRecipient cr
        where cr.capsuleId.capsuleId in :capsuleIds
    """)
    List<CapsuleRecipient> findAllByCapsuleIds(@Param("capsuleIds") List<Long> capsuleIds);


    @Query("SELECT c FROM CapsuleRecipient c WHERE c.capsuleId.capsuleId = :capsuleId")
    Optional<CapsuleRecipient> findByCapsuleId(@Param("capsuleId") Long capsuleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CapsuleRecipient cr where cr.capsuleId.capsuleId in :capsuleIds")
    int deleteByCapsuleIds(@Param("capsuleIds") List<Long> capsuleIds);
    // 수신 캡슐 월별 카운트
    @Query("SELECT month(cr.createdAt), count(cr) " +
            "FROM CapsuleRecipient cr " +
            "WHERE cr.recipientPhoneHash = :phoneHash " + // 수신자 식별 (해시된 폰번호)
            "AND year(cr.createdAt) = :year " +
            "AND cr.deletedAt IS NULL " +                // 삭제되지 않은 것만
            "GROUP BY month(cr.createdAt)")
    List<Object[]> countMonthlyReceiveCapsules(@Param("phoneHash") String phoneHash, @Param("year") int year);

    @Query("SELECT cr FROM CapsuleRecipient cr " +
            "JOIN FETCH cr.capsuleId c " +
            "WHERE cr.recipientPhoneHash = :phoneHash " +
            "AND c.unlockAt BETWEEN :startOfDay AND :endOfDay " +
            "AND cr.deletedAt IS NULL")
    List<CapsuleRecipient> findTodayUnlockedCapsules(
            @Param("phoneHash") String phoneHash,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
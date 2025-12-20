package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.CapsuleRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    void anonymizeByRecipientPhoneHash(@Param("phoneHash") String phoneHash);

    Optional<CapsuleRecipient> findByCapsuleId_CapsuleIdAndRecipientPhoneHash(Long capsuleId, String phoneHash);
    // phoneHash를 가지는 수신자 리스트 조회. JOIN을 이용하여 Capsule 테이블도 같이 조회
    @Query("SELECT cr FROM CapsuleRecipient cr JOIN FETCH cr.capsuleId c WHERE cr.recipientPhoneHash = :phoneHash")
    List<CapsuleRecipient> findAllByRecipientPhoneHashWithCapsule(@Param("phoneHash") String phoneHash);

    @Query("""
        select cr
        from CapsuleRecipient cr
        where cr.capsuleId.capsuleId in :capsuleIds
    """)
    List<CapsuleRecipient> findAllByCapsuleIds(@Param("capsuleIds") List<Long> capsuleIds);
}
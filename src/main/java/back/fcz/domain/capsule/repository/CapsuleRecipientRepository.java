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
}
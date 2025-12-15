package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.PublicCapsuleRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PublicCapsuleRecipientRepository extends JpaRepository<PublicCapsuleRecipient, Long> {
    @Query("""
    select p
    from PublicCapsuleRecipient p
    join p.capsuleId c
    join c.memberId m
    where c.capsuleId = :capsuleId
      and m.phoneHash = :phoneHash
""")
    Optional<PublicCapsuleRecipient> findByCapsuleIdAndPhoneHash(
            @Param("capsuleId") Long capsuleId,
            @Param("phoneHash") String phoneHash
    );

    boolean existsByCapsuleId_CapsuleId(Long capsuleId);

    // 특정 Capsule ID와 Member ID가 모두 일치하는 레코드가 존재하는지 확인.
    boolean existsByCapsuleId_CapsuleIdAndMemberId(Long capsuleId, Long memberId);
}

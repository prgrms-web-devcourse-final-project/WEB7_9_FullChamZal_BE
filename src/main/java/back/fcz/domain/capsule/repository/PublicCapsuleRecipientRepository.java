package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.PublicCapsuleRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicCapsuleRecipientRepository extends JpaRepository<PublicCapsuleRecipient, Long> {
    @Query("""
    select p
    from PublicCapsuleRecipient p
    join p.capsuleId c
    join c.memberId m
    where c.capsuleId = :capsuleId
      and m.phoneHash = :phoneHash
""")
    PublicCapsuleRecipient findByCapsuleIdAndPhoneHash(
            @Param("capsuleId") Long capsuleId,
            @Param("phoneHash") String phoneHash
    );
}

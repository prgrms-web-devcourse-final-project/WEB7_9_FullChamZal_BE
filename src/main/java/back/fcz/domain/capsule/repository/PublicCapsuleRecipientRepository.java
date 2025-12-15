package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.PublicCapsuleRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicCapsuleRecipientRepository extends JpaRepository<PublicCapsuleRecipient, Long> {

    boolean existsByCapsuleId_CapsuleId(Long capsuleId);

    // 특정 Capsule ID와 Member ID가 모두 일치하는 레코드가 존재하는지 확인.
    boolean existsByCapsuleId_CapsuleIdAndMemberId(Long capsuleId, Long memberId);
}

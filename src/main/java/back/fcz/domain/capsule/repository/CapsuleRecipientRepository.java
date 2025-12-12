package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.CapsuleRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CapsuleRecipientRepository extends JpaRepository<CapsuleRecipient,Long> {

    Optional<CapsuleRecipient> findByCapsuleId_CapsuleId(Long capsuleId);
}
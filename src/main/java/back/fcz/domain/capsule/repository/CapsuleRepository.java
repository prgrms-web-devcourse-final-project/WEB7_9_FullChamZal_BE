package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.Capsule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CapsuleRepository extends JpaRepository<Capsule, Long> {
    Optional<Capsule> findById(Long capsuleId);
}

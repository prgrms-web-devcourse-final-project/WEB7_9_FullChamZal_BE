package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.CapsuleOpenLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CapsuleOpenLogRepository extends JpaRepository<CapsuleOpenLog, Long>{

    Optional<CapsuleOpenLog> findFirstByCapsuleId_CapsuleIdAndMemberId_MemberIdOrderByOpenedAtAsc(Long capsuleId, Long memberId);
}

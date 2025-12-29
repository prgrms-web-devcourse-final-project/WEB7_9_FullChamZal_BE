package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.CapsuleOpenLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CapsuleOpenLogRepository extends JpaRepository<CapsuleOpenLog, Long>{

    Optional<CapsuleOpenLog> findFirstByCapsuleId_CapsuleIdAndMemberId_MemberIdOrderByOpenedAtAsc(Long capsuleId, Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CapsuleOpenLog col where col.capsuleId.capsuleId in :capsuleIds")
    int deleteByCapsuleIds(@Param("capsuleIds") List<Long> capsuleIds);
}

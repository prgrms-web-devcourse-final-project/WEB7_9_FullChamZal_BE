package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.CapsuleOpenLog;
import back.fcz.domain.capsule.entity.CapsuleOpenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CapsuleOpenLogRepository extends JpaRepository<CapsuleOpenLog, Long>{

    Optional<CapsuleOpenLog> findFirstByCapsuleId_CapsuleIdAndMemberIdOrderByOpenedAtAsc(Long capsuleId, Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CapsuleOpenLog col where col.capsuleId.capsuleId in :capsuleIds")
    int deleteByCapsuleIds(@Param("capsuleIds") List<Long> capsuleIds);

    // 특정 캡슐에 대한 특정 회원의 최근 로그 조회
    List<CapsuleOpenLog> findTop15ByCapsuleId_CapsuleIdAndMemberIdOrderByOpenedAtDesc(
            Long capsuleId,
            Long memberId
    );

    // 특정 IP 주소의 최근 로그 조회
    List<CapsuleOpenLog> findTop15ByCapsuleId_CapsuleIdAndIpAddressOrderByOpenedAtDesc(
            Long capsuleId,
            String ipAddress
    );

    // 성공 기록 확인 (회원용)
    boolean existsByCapsuleId_CapsuleIdAndMemberIdAndStatus(
            Long capsuleId,
            Long memberId,
            CapsuleOpenStatus status
    );

    // 성공 기록 확인 (비회원용)
    boolean existsByCapsuleId_CapsuleIdAndIpAddressAndStatus(
            Long capsuleId,
            String ipAddress,
            CapsuleOpenStatus status
    );
}

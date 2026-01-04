package back.fcz.domain.sanction.repository;

import back.fcz.domain.sanction.entity.MemberSanctionHistory;
import back.fcz.domain.sanction.entity.SanctionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberSanctionHistoryRepository extends JpaRepository<MemberSanctionHistory, Long> {
    Page<MemberSanctionHistory> findByMemberId(Long memberId, Pageable pageable);

    // 자동 해제 대상 제재 이력 조회
    @Query("""
        SELECT DISTINCT h FROM MemberSanctionHistory h
        WHERE h.sanctionType = :sanctionType
        AND h.sanctionUntil IS NOT NULL
        AND h.sanctionUntil <= :now
        AND h.afterStatus = back.fcz.domain.member.entity.MemberStatus.STOP
        AND NOT EXISTS (
            SELECT 1 FROM MemberSanctionHistory h2
            WHERE h2.memberId = h.memberId
            AND h2.sanctionType = back.fcz.domain.sanction.entity.SanctionType.RESTORE
            AND h2.createdAt > h.createdAt
        )
        ORDER BY h.memberId
    """)
    List<MemberSanctionHistory> findExpiredAutoSuspensions(
            @Param("sanctionType") SanctionType sanctionType,
            @Param("now") LocalDateTime now
    );
}

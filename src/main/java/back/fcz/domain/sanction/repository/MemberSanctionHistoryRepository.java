package back.fcz.domain.sanction.repository;

import back.fcz.domain.sanction.entity.MemberSanctionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSanctionHistoryRepository extends JpaRepository<MemberSanctionHistory, Long> {
    Page<MemberSanctionHistory> findByMemberId(Long memberId, Pageable pageable);
}

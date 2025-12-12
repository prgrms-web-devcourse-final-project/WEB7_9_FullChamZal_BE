package back.fcz.domain.member.repository;

import back.fcz.domain.member.entity.NicknameHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NicknameHistoryRepository extends JpaRepository<NicknameHistory, Long> {
}

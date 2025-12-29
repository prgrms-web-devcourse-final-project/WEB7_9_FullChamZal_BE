package back.fcz.domain.backup.repository;

import back.fcz.domain.backup.entity.Backup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BackupRepository extends JpaRepository<Backup, Long> {
    Optional<Backup> findByMemberId(Long memberId);
}

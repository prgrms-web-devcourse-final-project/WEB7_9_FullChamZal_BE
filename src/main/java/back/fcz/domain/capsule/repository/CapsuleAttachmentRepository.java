package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.CapsuleAttachment;
import back.fcz.domain.capsule.entity.CapsuleAttachmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CapsuleAttachmentRepository extends JpaRepository<CapsuleAttachment,Long> {
    Optional<CapsuleAttachment> findByIdAndUploaderId(Long id, Long uploaderId);

    List<CapsuleAttachment> findAllByCapsule_CapsuleIdAndStatus(Long capsuleId, CapsuleAttachmentStatus status);

    List<CapsuleAttachment> findTop1000ByStatusAndExpiredAtBeforeOrderByIdAsc(
            CapsuleAttachmentStatus status,
            LocalDateTime time
    );

    List<CapsuleAttachment> findTop1000ByStatusOrderByIdAsc(CapsuleAttachmentStatus status);
}
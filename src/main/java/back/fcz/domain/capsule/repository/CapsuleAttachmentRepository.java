package back.fcz.domain.capsule.repository;

import back.fcz.domain.capsule.entity.CapsuleAttachment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CapsuleAttachmentRepository extends JpaRepository<CapsuleAttachment, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CapsuleAttachment ca where ca.capsuleId.capsuleId in :capsuleIds")
    int deleteByCapsuleIds(@Param("capsuleIds") List<Long> capsuleIds);
}
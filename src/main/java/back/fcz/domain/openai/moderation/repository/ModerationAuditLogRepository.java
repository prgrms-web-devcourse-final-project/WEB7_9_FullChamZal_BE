package back.fcz.domain.openai.moderation.repository;

import back.fcz.domain.openai.moderation.entity.ModerationActionType;
import back.fcz.domain.openai.moderation.entity.ModerationAuditLog;
import back.fcz.domain.openai.moderation.entity.ModerationDecision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface ModerationAuditLogRepository extends JpaRepository<ModerationAuditLog, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update ModerationAuditLog m set m.capsuleId = :capsuleId where m.id = :auditId")
    int attachCapsuleId(@Param("auditId") Long auditId, @Param("capsuleId") Long capsuleId);

    @Query("""
            select m
            from ModerationAuditLog m
            where (:decision is null or m.decision = :decision)
              and (:actionType is null or m.actionType = :actionType)
              and (:actorMemberId is null or m.actorMemberId = :actorMemberId)
              and (:capsuleId is null or m.capsuleId = :capsuleId)
              and (:from is null or m.createdAt >= :from)
              and (:to is null or m.createdAt < :to)
            """)
    Page<ModerationAuditLog> search(
            @Param("decision") ModerationDecision decision,
            @Param("actionType") ModerationActionType actionType,
            @Param("actorMemberId") Long actorMemberId,
            @Param("capsuleId") Long capsuleId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ModerationAuditLog m where m.capsuleId in :capsuleIds")
    int deleteByCapsuleIdIn(@Param("capsuleIds") List<Long> capsuleIds);
}

package back.fcz.domain.report.repository;

import back.fcz.domain.report.entity.Report;
import back.fcz.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findAll(Pageable pageable);

    @Query("""
        select r
        from Report r
        where (:status is null or r.status = :status)
          and (:from is null or r.createdAt >= :from)
          and (:to is null or r.createdAt < :to)
        """)
    Page<Report> searchAdmin(
            @Param("status") ReportStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    long countByReporter_MemberId(Long memberId);

    long countByCapsule_CapsuleId(Long capsuleId);

    @Query("""
        select r.reporter.memberId, count(r)
        from Report r
        where r.reporter is not null
          and r.reporter.memberId in :memberIds
        group by r.reporter.memberId
        """)
    List<Object[]> countByReporterMemberIds(@Param("memberIds") List<Long> memberIds);

    @Query("""
        select r.capsule.capsuleId, count(r)
        from Report r
        where r.capsule.capsuleId in :capsuleIds
        group by r.capsule.capsuleId
        """)
    List<Object[]> countByCapsuleIds(@Param("capsuleIds") List<Long> capsuleIds);
}

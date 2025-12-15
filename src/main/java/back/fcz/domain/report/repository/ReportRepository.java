package back.fcz.domain.report.repository;

import back.fcz.domain.report.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // TODO: status, targetType, createdAt 범위 등 조건 검색은 나중에 Querydsl로 확장
    Page<Report> findAll(Pageable pageable);
}

package amgn.amu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import amgn.amu.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Report> findByStatusOrderByCreatedAtDesc(Report.ReportStatus status, Pageable pageable);
}

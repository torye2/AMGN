package amgn.amu.repository;

import amgn.amu.entity.ReportEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportEvidenceRepository extends JpaRepository<ReportEvidence, Long> {
    List<ReportEvidence> findByReportIdOrderByCreatedAtDesc(Long reportId);
    long countByReportId(Long reportId);
}

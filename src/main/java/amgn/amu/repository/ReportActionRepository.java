package amgn.amu.repository;

import amgn.amu.entity.ReportAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportActionRepository extends JpaRepository<ReportAction, Long> {
    List<ReportAction> findByReportIdOrderByCreatedAtDesc(Long reportId);
}

package amgn.amu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import amgn.amu.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(String status);
}

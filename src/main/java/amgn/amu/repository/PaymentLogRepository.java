package amgn.amu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import amgn.amu.entity.PaymentLog;

import java.util.List;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    List<PaymentLog> findByOrderId(Long id);
}

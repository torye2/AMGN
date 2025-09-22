package amgn.amu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import amgn.amu.entity.PaymentLog;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}

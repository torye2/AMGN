package amgn.amu.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "payment_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentType type; // PAY, REFUND, TRANSFER

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status; // PENDING, SUCCESS, FAILED

    @Column(name = "pg_transaction_id")
    private String pgTransactionId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "merchant_uid")
    private String merchantUid;

    @Column(name = "imp_uid")
    private String impUid;
}

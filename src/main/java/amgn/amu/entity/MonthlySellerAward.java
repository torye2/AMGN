package amgn.amu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "monthly_seller_awards")
public class MonthlySellerAward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "award_id")
    private Long awardId;

    @Column(name = "ym", nullable = false)
    private LocalDate ym;                 // 그 달의 1일

    @Column(name = "seller_id", nullable = false)
    private Integer sellerId;

    @Column(name = "metric", nullable = false)
    private String metric;                // 'gmv' or 'orders'

    @Column(name = "value_num", nullable = false)
    private BigDecimal valueNum;

    @Column(name = "rank_num", nullable = false)
    private Integer rankNum;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

}

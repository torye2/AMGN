package amgn.amu.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "rater_id", nullable = false)
    private Long raterId;

    @Column(name = "ratee_id", nullable = false)
    private Long rateeId;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "rv_comment")
    private String rvComment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

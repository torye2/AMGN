package amgn.amu.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "rater_id")
    private Long raterId;

    @Column(name = "ratee_id")
    private Long rateeId;

    @Column(name = "score")
    private Integer score;

    @Column(name = "rv_comment")
    private String rvComment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

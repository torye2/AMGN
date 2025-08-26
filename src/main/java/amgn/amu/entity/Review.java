package amgn.amu.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "rater_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long raterId;

    @Column(nullable = false)
    private Long rateeId;

    @Column(nullable = false)
    private int score;

    @Column(length = 500)
    private String rvComment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

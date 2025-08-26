package amgn.amu.dto;

import java.time.LocalDateTime;

public record ReviewDto(
    Long reviewId,
    Long orderId,
    Long raterId,
    Long rateeId,
    int score,
    String rvComment,
    LocalDateTime createdAt
) {}

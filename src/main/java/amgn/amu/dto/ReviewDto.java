package amgn.amu.dto;

import java.time.LocalDateTime;

public record ReviewDto(
        Long id,
        Long orderId,
        Long raterId,
        Integer score,
        String rvComment,
        LocalDateTime createdAt
) {}


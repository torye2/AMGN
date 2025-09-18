package amgn.amu.dto;

import java.time.LocalDateTime;

public record ReviewSummaryItemDto(
        Long reviewId,
        Integer rating,
        String comment,
        String reviewerNickname,
        LocalDateTime createdAt
) {}

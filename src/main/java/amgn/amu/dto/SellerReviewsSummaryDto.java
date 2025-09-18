package amgn.amu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SellerReviewsSummaryDto(
        double avgRating,
        long reviewCount,
        List<ReviewSummaryItemDto> items
) {}
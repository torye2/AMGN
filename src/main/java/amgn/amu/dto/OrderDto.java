package amgn.amu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long id,
        Long listingId,
        Long buyerId,
        String buyerName,
        Long sellerId,
        Long finalPrice,
        TradeMethod method,
        PaymentRequest.PaymentMethod paymentMethod,
        OrderStatus status,
        LocalDateTime createdAt,
        String listingTitle, // Listing 제목
        String transferStatus,
        List<PaymentLogDto> paymentLogs
//      ,  boolean reviewed,            // 리뷰 여부
//        List<ReviewDto> reviews      // 리뷰 목록
) {

    public enum OrderStatus {
        CREATED,
        PAID,
        IN_TRANSIT,
        DELIVERED,
        MEETUP_CONFIRMED,
        CANCELLED,
        DISPUTED,
        REFUNDED,
        COMPLETED
    }

    public enum TradeMethod {
        MEETUP,
        DELIVERY
    }

    public record ReviewDto(
            Long reviewId,
            Long raterId,
            String raterNickName,
            String rvComment,
            int score,
            LocalDateTime createdAt
    ) {}
}

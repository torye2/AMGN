package amgn.amu.dto;

import java.time.LocalDateTime;


public record OrderDto(
        Long id,
        Long listingId,
        Long buyerId,
        Long sellerId,
        Long finalPrice,
        TradeMethod method,
        OrderStatus status,
        LocalDateTime createdAt
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
		MEETUP, // 직거래
		DELIVERY, // 택배 배송
		// ESCROW_DELIVERY 안전거래 + 배송 추후에 확장하면 넣을 기능(비대면 안전 거래. 돈을 제 3의 회사에서 가지고 있다가 택배 확인 후 송금이 완료되는 기능)
	}
}
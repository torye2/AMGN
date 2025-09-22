package amgn.amu.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
		@NotNull Long orderId,                 // 주문 ID
		@NotNull Long amount,                  // 결제 금액
		@NotNull PaymentMethod method,         // 결제 수단
		@NotNull String idempotencyKey,        // 멱등성 키
		@NotNull LocalDateTime expiryDate      // 결제 만료 시간

) {
	public enum PaymentMethod {
		KG_INICIS,
		TOSS,
		KAKAO
	}
}

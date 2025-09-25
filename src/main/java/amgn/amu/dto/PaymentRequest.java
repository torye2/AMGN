package amgn.amu.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
		@NotNull Long orderId,                 // 주문 ID
		@NotNull Long amount,                  // 결제 금액
		@NotNull @JsonProperty("paymentMethod") PaymentMethod method, // 결제 수단
		@NotNull String idempotencyKey,       // 멱등성 키
		@NotNull LocalDateTime expiryDate,     // 결제 만료 시간
		String merchantUid,                    // PG사 주문 ID 등
		String impUid,                         // 결제 PG사 고유 ID
		String pgTransactionId                 // 환불/송금 트랜잭션 ID
) {
	public enum PaymentMethod {
		KG_INICIS,
		TOSS,
		KAKAO
	}
}

package amgn.amu.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

// LocalDateTime 키 만료 시간
public record PaymentRequest(@NotNull String idempotencyKey// 멱등성 키
								,@NotNull String response// 이전 요청 처리 결과
								,@NotNull LocalDateTime expiryDate) {
    

}


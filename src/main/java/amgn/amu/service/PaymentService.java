package amgn.amu.service;

import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.PaymentResponse;
import amgn.amu.dto.OrderDto;
import jakarta.validation.constraints.NotNull;

public interface PaymentService {
    PaymentResponse preparePayment(Long userId, @NotNull Long orderId);
    PaymentResponse completePayment(Long userId, PaymentRequest req);

    OrderDto payWithKakao(Long userIdFromSession, Long orderId, PaymentRequest req);
    OrderDto payWithInicis(Long userIdFromSession, Long orderId, PaymentRequest req);

    String testPreRegister(Long orderId, Long amount);
}

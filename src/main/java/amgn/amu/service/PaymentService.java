package amgn.amu.service;

import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse preparePayment(Long userId, PaymentRequest req);
    PaymentResponse completePayment(Long userId, PaymentRequest req);
}

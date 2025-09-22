package amgn.amu.service;

import amgn.amu.dto.PaymentRequest;


public interface PaymentGateway {
    boolean pay(PaymentRequest req);      // 결제
    boolean refund(PaymentRequest req);   // 환불
}

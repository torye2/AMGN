package amgn.amu.service;

import org.springframework.stereotype.Service;
import amgn.amu.dto.PaymentRequest;

@Service
public class TossPaymentGateway implements PaymentGateway {
    @Override
    public boolean pay(PaymentRequest req) {
        System.out.println("토스페이먼츠 결제 요청: " + req);
        return true;
    }

    @Override
    public boolean refund(PaymentRequest req) {
        System.out.println("토스페이먼츠 환불 요청: " + req);
        return true;
    }
}

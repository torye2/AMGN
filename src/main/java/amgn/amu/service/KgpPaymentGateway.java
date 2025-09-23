package amgn.amu.service;

import amgn.amu.dto.PaymentRequest;
import org.springframework.stereotype.Service;


public class KgpPaymentGateway implements PaymentGateway {

    @Override
    public boolean pay(PaymentRequest req) {
        // 실제 KG_INICIS 결제 API 호출
        System.out.println("KG_INICIS 결제 처리: " + req.orderId());
        return true;
    }

    @Override
    public boolean refund(PaymentRequest req) {
        // 실제 KG_INICIS 환불 API 호출
        System.out.println("KG_INICIS 환불 처리: " + req.orderId());
        return true;
    }
}

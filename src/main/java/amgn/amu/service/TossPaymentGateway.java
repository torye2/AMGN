package amgn.amu.service;

import amgn.amu.dto.PaymentRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Map;

@Service
public class TossPaymentGateway implements PaymentGateway {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com/v1/payments")
            .build();

    private static final String SECRET_KEY = "YOUR_TOSS_SECRET_KEY";

    @Override
    public boolean pay(PaymentRequest req) {
        System.out.println("Toss 결제 처리(테스트용): " + req.orderId());
        return true;
    }

    @Override
    public boolean refund(PaymentRequest req) {
        try {
            Map<String, Object> body = Map.of(
                    "cancelReason", "주문 취소",
                    "cancelAmount", req.amount()
            );

            Map<String, Object> response = webClient.post()
                    .uri("/{paymentKey}/cancel", req.impUid())
                    .header("Authorization", "Basic " + SECRET_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("status") && response.get("status").equals("CANCELED")) {
                System.out.println("Toss 환불 성공: " + req.orderId());
                return true;
            } else {
                System.err.println("Toss 환불 실패: " + response);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

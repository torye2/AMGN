package amgn.amu.service;

import amgn.amu.dto.PaymentRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class KgpPaymentGateway implements PaymentGateway {

    private static final String IMP_KEY = "YOUR_IMP_KEY";       // 아임포트 REST API 키
    private static final String IMP_SECRET = "YOUR_IMP_SECRET"; // 아임포트 REST API 시크릿

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.iamport.kr")
            .build();

    private String getAccessToken() {
        Map<String, String> body = Map.of(
                "imp_key", IMP_KEY,
                "imp_secret", IMP_SECRET
        );

        Map<String, Object> response = webClient.post()
                .uri("/users/getToken")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && (Integer) response.get("code") == 0) {
            Map<String, Object> res = (Map<String, Object>) response.get("response");
            return (String) res.get("access_token");
        } else {
            throw new RuntimeException("아임포트 토큰 발급 실패: " + response);
        }
    }

    @Override
    public boolean pay(PaymentRequest req) {
        // 실제 결제는 OrderService에서 테스트 환경 여부로 처리함
        System.out.println("KG_INICIS 결제 처리(실제 호출 필요 시 구현): " + req.orderId());
        return true;
    }

    @Override
    public boolean refund(PaymentRequest req) {
        try {
            String token = getAccessToken();

            Map<String, Object> body = Map.of(
                    "merchant_uid", req.merchantUid(),
                    "amount", req.amount(),
                    "reason", "주문 취소"
            );

            Map<String, Object> response = webClient.post()
                    .uri("/payments/cancel")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && (Integer) response.get("code") == 0) {
                System.out.println("환불 성공: " + req.orderId());
                return true;
            } else {
                System.err.println("환불 실패: " + response);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

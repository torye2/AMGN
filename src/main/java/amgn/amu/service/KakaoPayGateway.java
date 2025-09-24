package amgn.amu.service;

import amgn.amu.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoPayGateway implements PaymentGateway {

    @Value("${kakao.admin-key}")
    private String adminKey; // application.properties에 테스트 키 저장

    private final WebClient webClient = WebClient.create("https://kapi.kakao.com");

    private String accessToken; // 간단 테스트용, 실제론 토큰 관리 필요

    @Override
    public boolean pay(PaymentRequest req) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("cid", "TC0ONETIME"); // 테스트 상점 코드
            body.put("partner_order_id", req.orderId().toString());
            body.put("partner_user_id", req.orderId().toString()); // 예시로 userId 대신 orderId
            body.put("item_name", "테스트상품");
            body.put("quantity", 1);
            body.put("total_amount", req.amount());
            body.put("vat_amount", 0);
            body.put("tax_free_amount", 0);
            body.put("approval_url", "http://localhost:8080/payment/success"); // 테스트 완료 URL
            body.put("cancel_url", "http://localhost:8080/payment/cancel");
            body.put("fail_url", "http://localhost:8080/payment/fail");

            Map<String, Object> response = webClient.post()
                    .uri("/v1/payment/ready")
                    .header("Authorization", "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(convertToFormData(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            System.out.println("KakaoPay 준비 응답: " + response);
            // response에서 next_redirect_pc_url을 프론트에 전달
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean refund(PaymentRequest req) {
        // 실제 환불 구현 시 KakaoPay API 호출
        System.out.println("카카오페이 환불 요청: " + req);
        return true;
    }

    // ----------------- Helper -----------------
    private String convertToFormData(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> {
            if (sb.length() > 0) sb.append("&");
            sb.append(k).append("=").append(v);
        });
        return sb.toString();
    }
}

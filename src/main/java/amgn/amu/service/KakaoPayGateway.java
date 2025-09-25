package amgn.amu.service;

import amgn.amu.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoPayGateway implements PaymentGateway {

    @Value("${kakao.admin-key}")
    private String adminKey; // application.properties에 테스트 키 저장

    private final WebClient webClient = WebClient.create("https://kapi.kakao.com");

    @Override
    public boolean pay(PaymentRequest req) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("cid", "TC0ONETIME"); // 테스트 상점 코드
            body.put("partner_order_id", req.orderId().toString());
            body.put("partner_user_id", req.orderId().toString());
            body.put("item_name", "테스트상품");
            body.put("quantity", "1");
            body.put("total_amount", String.valueOf(req.amount()));
            body.put("vat_amount", "0");
            body.put("tax_free_amount", "0");
            body.put("approval_url", "http://localhost:8080/payment/success");
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

            if (response != null && response.containsKey("tid")) {
                String redirectUrl = (String) response.get("next_redirect_pc_url");
                System.out.println("KakaoPay 준비 완료: " + redirectUrl);
                // TODO: 프론트에 redirectUrl 반환
                return true;
            } else {
                System.err.println("KakaoPay 준비 실패: " + response);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean refund(PaymentRequest req) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("cid", "TC0ONETIME");
            body.put("tid", req.impUid()); // 결제 승인 시 받은 TID
            body.put("cancel_amount", String.valueOf(req.amount()));
            body.put("cancel_reason", "주문 취소");

            Map<String, Object> response = webClient.post()
                    .uri("/v1/payment/cancel")
                    .header("Authorization", "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(convertToFormData(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && (Integer) response.get("code") == 0) {
                System.out.println("KakaoPay 환불 성공: " + req.orderId());
                return true;
            } else {
                System.err.println("KakaoPay 환불 실패: " + response);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ----------------- Helper -----------------
    private String convertToFormData(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> {
            if (sb.length() > 0) sb.append("&");
            sb.append(k).append("=").append(v);
        });
        return sb.toString();
    }
}

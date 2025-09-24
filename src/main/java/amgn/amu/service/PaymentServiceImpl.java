package amgn.amu.service;

import amgn.amu.dto.OrderDto;
import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.PaymentResponse;
import amgn.amu.entity.PaymentLog;
import amgn.amu.repository.PaymentLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderService orderService;
    private final PaymentLogRepository paymentLogRepository;
    private final KgpPaymentGateway kgpPaymentGateway;
    private final TossPaymentGateway tossPaymentGateway;
    private final KakaoPayGateway kakaoPayGateway;

    // WebClient 직접 생성
    private final WebClient webClient = WebClient.create("https://api.iamport.kr");

    @Value("${imp.key}")
    private String IMP_KEY;

    @Value("${imp.secret}")
    private String IMP_SECRET;

    @Override
    public PaymentResponse preparePayment(Long userId, Long orderId) {
        OrderDto order = orderService.getOrder(userId, orderId);
        if (order == null) throw new RuntimeException("주문을 찾을 수 없습니다.");
        if (order.status() != OrderDto.OrderStatus.CREATED)
            throw new RuntimeException("결제 가능한 상태가 아닙니다.");

        String merchantUid = "order_" + order.id() + "_" + System.currentTimeMillis();

        return new PaymentResponse(
                true,
                "결제 준비 완료",
                merchantUid,
                order.finalPrice(),
                order.listingTitle()
        );
    }

    @Override
    public PaymentResponse completePayment(Long userId, PaymentRequest req) {
        OrderDto order = orderService.getOrder(userId, req.orderId());
        if (order == null) throw new RuntimeException("주문을 찾을 수 없습니다.");
        if (order.status() != OrderDto.OrderStatus.CREATED)
            throw new RuntimeException("결제 가능한 상태가 아닙니다.");
        if (!req.amount().equals(order.finalPrice()))
            throw new RuntimeException("결제 금액이 주문 금액과 다릅니다.");

        if (paymentLogRepository.existsByIdempotencyKey(req.idempotencyKey()))
            throw new RuntimeException("이미 처리된 결제입니다.");

        boolean success = switch (req.method()) {
            case KG_INICIS -> kgpPaymentGateway.pay(req);
            case TOSS -> tossPaymentGateway.pay(req);
            case KAKAO -> kakaoPayGateway.pay(req);
        };

        if (!success) throw new RuntimeException("결제 실패");

        // 주문 상태 업데이트
        orderService.pay(userId, order.id(), req);

        // 결제 로그 저장
        PaymentLog log = new PaymentLog();
        log.setOrderId(order.id());
        log.setType("PAY");
        log.setIdempotencyKey(req.idempotencyKey());
        log.setCreatedAt(LocalDateTime.now());
        paymentLogRepository.save(log);

        return new PaymentResponse(
                true,
                "결제가 완료되었습니다.",
                req.merchantUid(),
                order.finalPrice(),
                order.listingTitle()
        );
    }

    @Override
    public OrderDto payWithKakao(Long userIdFromSession, Long orderId, PaymentRequest req) {
        kakaoPayGateway.pay(req);
        return orderService.pay(userIdFromSession, orderId, req);
    }

    @Override
    public OrderDto payWithInicis(Long userIdFromSession, Long orderId, PaymentRequest req) {
        kgpPaymentGateway.pay(req);
        return orderService.pay(userIdFromSession, orderId, req);
    }

    // 테스트용: 사전등록 API 호출
    public String testPreRegister(Long orderId, Long amount) {
        return webClient.post()
                .uri("/payments/prepare")
                .headers(h -> h.setBasicAuth(IMP_KEY, IMP_SECRET))
                .bodyValue(Map.of(
                        "merchant_uid", "order_" + orderId,
                        "amount", amount
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}

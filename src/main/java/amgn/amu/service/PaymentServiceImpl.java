package amgn.amu.service;

import amgn.amu.dto.OrderDto;
import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.PaymentResponse;
import amgn.amu.entity.PaymentLog;
import amgn.amu.repository.PaymentLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderService orderService;
    private final KgpPaymentGateway kgpPaymentGateway;
    private final TossPaymentGateway tossPaymentGateway;
    private final KakaoPayGateway kakaoPayGateway;
    private final PaymentLogRepository paymentLogRepository;

    @Override
    public PaymentResponse preparePayment(Long userId, PaymentRequest req) {
        OrderDto order = orderService.getOrder(userId, req.orderId());
        if (order == null) throw new RuntimeException("주문을 찾을 수 없습니다.");
        if (order.status() != OrderDto.OrderStatus.CREATED)
            throw new RuntimeException("결제 가능한 상태가 아닙니다.");

        String merchantUid = "order_" + order.id() + "_" + System.currentTimeMillis();
        return new PaymentResponse(true, "결제 준비 완료", merchantUid, order.finalPrice(), order.listingTitle());
    }

    @Override
    public PaymentResponse completePayment(Long userId, PaymentRequest req) {
        OrderDto order = orderService.getOrder(userId, req.orderId());
        if (order == null) throw new RuntimeException("주문을 찾을 수 없습니다.");
        if (order.status() != OrderDto.OrderStatus.CREATED)
            throw new RuntimeException("결제 가능한 상태가 아닙니다.");
        if (!req.amount().equals(order.finalPrice()))
            throw new RuntimeException("결제 금액이 주문 금액과 다릅니다.");

        // 멱등성 체크
        if (paymentLogRepository.existsByIdempotencyKey(req.idempotencyKey())) {
            throw new RuntimeException("이미 처리된 결제입니다.");
        }

        boolean success = false;
        switch (req.method()) {
            case KG_INICIS:
                success = kgpPaymentGateway.pay(req);
                break;
            case TOSS:
                success = tossPaymentGateway.pay(req);
                break;
            case KAKAO:
                success = kakaoPayGateway.pay(req);
                break;
        }

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

        String merchantUid = "order_" + order.id() + "_" + System.currentTimeMillis();
        return new PaymentResponse(true, "결제가 완료되었습니다.", merchantUid, order.finalPrice(), order.listingTitle());
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
}

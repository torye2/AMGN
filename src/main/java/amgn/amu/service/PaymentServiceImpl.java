package amgn.amu.service;

import amgn.amu.dto.OrderDto;
import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderService orderService;

    // 결제 준비 (merchant_uid 생성, 금액 검증)
    @Override
    public PaymentResponse preparePayment(Long userId, PaymentRequest req) {
        // 주문 조회
        OrderDto order = orderService.getOrder(userId, req.orderId());

        if (order == null) throw new RuntimeException("주문을 찾을 수 없습니다.");
        // 주문 상태 확인
        if (order.status() != OrderDto.OrderStatus.CREATED)
            throw new RuntimeException("결제 가능한 상태가 아닙니다.");

        // merchantUid 생성
        String merchantUid = "order_" + order.id() + "_" + System.currentTimeMillis();

        return new PaymentResponse(
                true,
                "결제 준비 완료",
                merchantUid,
                order.finalPrice(),
                order.listingTitle()
        );
    }

    // 결제 완료 확인
    @Override
    public PaymentResponse completePayment(Long userId, PaymentRequest req) {
        // 주문 조회
        OrderDto order = orderService.getOrder(userId, req.orderId());

        if (order == null) throw new RuntimeException("주문을 찾을 수 없습니다.");
        // 주문 상태 확인
        if (order.status() != OrderDto.OrderStatus.CREATED)
            throw new RuntimeException("결제 가능한 상태가 아닙니다.");

        // 금액 검증
        if (!req.amount().equals(order.finalPrice())) {
            throw new RuntimeException("결제 금액이 주문 금액과 다릅니다.");
        }

        // 결제 처리
        orderService.pay(userId, order.id(), req); // 실제 결제 처리

        // merchantUid 생성
        String merchantUid = "order_" + order.id() + "_" + System.currentTimeMillis();

        return new PaymentResponse(
                true,
                "결제가 완료되었습니다.",
                merchantUid,
                order.finalPrice(),
                order.listingTitle()
        );
    }
}

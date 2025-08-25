package amgn.amu.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import amgn.amu.dto.OrderCreateRequest;
import amgn.amu.dto.OrderDto;
import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.TrackingInputRequest;
import amgn.amu.entity.Order;
import amgn.amu.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    public OrderDto create(Long actorUserId, OrderCreateRequest req) {
        Order order = new Order();
        order.setBuyerId(actorUserId);
        order.setListingId(req.listingId());
        order.setFinalPrice(0L); // 예: 나중에 계산
        order.setMethod(req.method());
        order.setStatus(OrderDto.OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());

        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto pay(Long buyerId, Long orderId, PaymentRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문 없음"));
        if (!order.getBuyerId().equals(buyerId))
            throw new RuntimeException("권한 없음");

        order.setStatus(OrderDto.OrderStatus.PAID);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto confirmMeetup(Long actorUserId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문 없음"));
        order.setStatus(OrderDto.OrderStatus.MEETUP_CONFIRMED);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto inputTracking(Long sellerId, Long orderId, TrackingInputRequest r) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문 없음"));
        order.setStatus(OrderDto.OrderStatus.IN_TRANSIT);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto confirmDelivered(Long buyerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문 없음"));
        order.setStatus(OrderDto.OrderStatus.DELIVERED);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto complete(Long actorUserId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문 없음"));
        order.setStatus(OrderDto.OrderStatus.COMPLETED);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto cancel(Long actorUserId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문 없음"));
        order.setStatus(OrderDto.OrderStatus.CANCELLED);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto dispute(Long actorUserId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문 없음"));
        order.setStatus(OrderDto.OrderStatus.DISPUTED);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public List<OrderDto> myOrders(Long userId) {
        List<Order> orders = orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId);
        return orders.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Order 엔티티를 OrderDto로 변환하는 메서드 (/src/main/java/amgn/amu/entity/Order.java 참고)
     * - 엔티티는 DB와 직접 연결되어 있는 객체
     * - DTO는 외부로 데이터를 전달할 때 사용되는 객체
     * 
     * @param order 변환할 Order 엔티티
     * @return OrderDto 엔티티의 값을 담은 DTO
     */
    private OrderDto toDto(Order order) {
        return new OrderDto(
                order.getId(),           // 주문 ID
                order.getListingId(),    // 상품/리스트 ID
                order.getBuyerId(),      // 구매자 ID
                order.getSellerId(),     // 판매자 ID
                order.getFinalPrice(),   // 최종 가격
                order.getMethod(),       // 거래 방식 (직거래/배송 등)
                order.getStatus(),       // 주문 상태 (CREATED, PAID, DELIVERED 등)
                order.getCreatedAt()     // 주문 생성 시간
        );
    }
}

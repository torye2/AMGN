package amgn.amu.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import amgn.amu.dto.ListingDto;
import amgn.amu.dto.OrderCreateRequest;
import amgn.amu.dto.OrderDto;
import amgn.amu.dto.OrderDto.OrderStatus;
import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.TrackingInputRequest;
import amgn.amu.entity.Listing;
import amgn.amu.entity.Order;
import amgn.amu.entity.PaymentLog;
import amgn.amu.repository.ListingRepository;
import amgn.amu.repository.OrderRepository;
import amgn.amu.repository.PaymentLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;
    private final PaymentLogRepository paymentLogRepository;

    // PG사 DI
    private final KgpPaymentGateway kgpPaymentGateway;
    private final TossPaymentGateway tossPaymentGateway;
    private final KakaoPayGateway kakaoPayGateway;

    @Override
    public boolean isListingInTransaction(Long listingId) {
        return orderRepository.existsByListingIdAndStatusIn(
                listingId, List.of(OrderStatus.CREATED, OrderStatus.IN_TRANSIT));
    }

    private PaymentGateway selectGateway(PaymentRequest.PaymentMethod method) {
        return switch (method) {
            case KG_INICIS -> kgpPaymentGateway;
            case TOSS -> tossPaymentGateway;
            case KAKAO -> kakaoPayGateway;
        };
    }

    private PaymentRequest.PaymentMethod mapTradeMethodToPayment(OrderDto.TradeMethod method) {
        return PaymentRequest.PaymentMethod.KG_INICIS; // 필요 시 매핑 로직 수정
    }

    @Override
    public OrderDto create(Long actorUserId, OrderCreateRequest req) {
        Listing listing = listingRepository.findById(req.listingId())
                .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다: " + req.listingId()));

        if (listing.getSellerId().equals(actorUserId)) {
            throw new RuntimeException("본인 상품은 주문할 수 없습니다.");
        }

        if (orderRepository.existsByListingIdAndStatusIn(
                req.listingId(),
                List.of(OrderStatus.CREATED, OrderStatus.PAID, OrderStatus.IN_TRANSIT, OrderStatus.MEETUP_CONFIRMED))) {
            throw new RuntimeException("이미 거래 중인 상품입니다.");
        } else if (orderRepository.existsByListingIdAndStatusIn(req.listingId(), List.of(OrderStatus.COMPLETED))) {
            throw new RuntimeException("이미 판매가 완료된 상품입니다.");
        }

        Order order = new Order();
        order.setBuyerId(actorUserId);
        order.setListingId(req.listingId());
        order.setSellerId(listing.getSellerId());
        order.setFinalPrice(listing.getPrice().longValue());
        order.setMethod(req.method());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        order.setReceiverName(req.recvName());
        order.setReceiverPhone(req.recvPhone());
        order.setReceiverAddress1(req.recvAddr1());
        order.setReceiverAddress2(req.recvAddr2());
        order.setReceiverZip(req.recvZip());

        // 결제 수단 처리 (기본값 KG_INICIS)
        PaymentRequest.PaymentMethod paymentMethod;
        if (req.paymentMethod() == null || req.paymentMethod().isBlank()) {
            paymentMethod = PaymentRequest.PaymentMethod.KG_INICIS;
        } else {
            try {
                paymentMethod = PaymentRequest.PaymentMethod.valueOf(req.paymentMethod());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("유효하지 않은 결제 수단입니다: " + req.paymentMethod());
            }
        }
        order.setPaymentMethod(paymentMethod);

        orderRepository.save(order);
        updateListingStatus(order.getListingId(), order.getStatus());

        return toDto(order);
    }


    // ---------------- 결제 ----------------
    // 결제 처리 로직
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderDto pay(Long buyerId, Long orderId, PaymentRequest req) {
        // 주문 및 구매자 검증
        Order order = findOrderByIdAndCheckBuyer(orderId, buyerId);

        // 상태 및 금액 검증
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new RuntimeException("결제할 수 없는 상태입니다.");
        }

        if (!req.amount().equals(order.getFinalPrice())) {
            throw new RuntimeException("결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        // 중복 결제 체크
        if (isDuplicatePayment(req.idempotencyKey())) {
            throw new RuntimeException("중복 결제입니다.");
        }

        // 결제 처리
        PaymentGateway gateway = selectGateway(req.method());
        boolean success = gateway.pay(req);
        if (!success) {
            throw new RuntimeException("결제 실패");
        }

        // 결제 성공 시 상태 변경 및 로그 기록
        order.setPaymentMethod(req.method());
        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 결제 로그 기록
        paymentLogRepository.save(new PaymentLog(null, req.idempotencyKey(), orderId, "PAY", LocalDateTime.now()));

        // 상품 상태 업데이트
        updateListingStatus(order.getListingId(), order.getStatus());

        return toDto(order);
    }

    private boolean isDuplicatePayment(String idempotencyKey) {
        return paymentLogRepository.existsByIdempotencyKey(idempotencyKey);
    }


    @Override
    public OrderDto confirmMeetup(Long actorUserId, Long orderId) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.MEETUP_CONFIRMED);
        orderRepository.save(order);
        updateListingStatus(order.getListingId(), order.getStatus());
        return toDto(order);
    }

    @Override
    public OrderDto inputTracking(Long sellerId, Long orderId, TrackingInputRequest r) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.IN_TRANSIT);
        orderRepository.save(order);
        updateListingStatus(order.getListingId(), order.getStatus());
        return toDto(order);
    }

    @Override
    public OrderDto confirmDelivered(Long buyerId, Long orderId) {
        Order order = findOrderByIdAndCheckBuyer(orderId, buyerId);
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
        updateListingStatus(order.getListingId(), order.getStatus());
        return toDto(order);
    }

    @Override
    public OrderDto complete(Long actorUserId, Long orderId) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        updateListingStatus(order.getListingId(), order.getStatus());
        return toDto(order);
    }

    @Override
    public OrderDto cancel(Long actorUserId, Long orderId) {
        Order order = findOrderById(orderId);
        String merchantUid = "refund_" + order.getId() + "_" + System.currentTimeMillis();
        String impUidFromPayment = order.getImpUid(); // Order 엔티티에 impUid가 저장되어 있어야 함


        if (!order.getBuyerId().equals(actorUserId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("이미 완료된 주문은 취소할 수 없습니다.");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            PaymentRequest refundReq = new PaymentRequest(
                    order.getId(),
                    order.getFinalPrice(),
                    order.getPaymentMethod(),
                    "refund_" + order.getId(),
                    LocalDateTime.now().plusHours(1),
                    merchantUid,                           // merchantUid
                    impUidFromPayment

            );

            PaymentGateway gateway = selectGateway(refundReq.method());
            boolean refunded = gateway.refund(refundReq);
            if (!refunded) throw new RuntimeException("환불 실패");

            paymentLogRepository.save(new PaymentLog(null, refundReq.idempotencyKey(), orderId, "REFUND", LocalDateTime.now()));
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        updateListingStatus(order.getListingId(), order.getStatus());

        return toDto(order);
    }

    @Override
    public OrderDto dispute(Long actorUserId, Long orderId, String reason) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.DISPUTED);
        orderRepository.save(order);
        updateListingStatus(order.getListingId(), order.getStatus());
        return toDto(order);
    }

    @Override
    public List<OrderDto> myOrders(Long userId) {
        return orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId).stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ListingDto getListingInfo(Long listingId) {
        return listingRepository.findById(listingId)
                .map(this::toListingDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteOrder(Long userId, Long orderId) {
        Order order = findOrderById(orderId);
        if (!order.getBuyerId().equals(userId)) throw new RuntimeException("권한이 없습니다.");
        updateListingStatus(order.getListingId(), OrderStatus.CANCELLED);
        orderRepository.delete(order);
    }

    @Override
    public OrderDto revertCancel(Long userId, Long orderId) {
        Order order = findOrderById(orderId);
        if (!order.getBuyerId().equals(userId)) throw new RuntimeException("권한이 없습니다.");
        if (order.getStatus() != OrderStatus.PAID) throw new RuntimeException("주문에 오류가 생겨 복원할 수 없습니다.");
        order.setStatus(OrderStatus.CREATED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        updateListingStatus(order.getListingId(), order.getStatus());
        return toDto(order);
    }

    // ---------------- 헬퍼 ----------------
    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문이 존재하지 않습니다: " + orderId));
    }

    private Order findOrderByIdAndCheckBuyer(Long orderId, Long buyerId) {
        Order order = findOrderById(orderId);
        if (!order.getBuyerId().equals(buyerId)) throw new RuntimeException("권한이 없습니다.");
        return order;
    }

    private OrderDto toDto(Order order) {
        String title = listingRepository.findById(order.getListingId())
                .map(Listing::getTitle)
                .orElse("-");
        return new OrderDto(
                order.getId(),
                order.getListingId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getFinalPrice(),
                order.getMethod(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getCreatedAt(),
                title
        );
    }

    private ListingDto toListingDto(Listing listing) {
        ListingDto dto = new ListingDto();
        dto.setListingId(listing.getListingId());
        dto.setSellerId(listing.getSellerId());
        dto.setTitle(listing.getTitle());
        dto.setPrice(listing.getPrice());
        dto.setNegotiable(listing.getNegotiable());
        dto.setCategoryId(listing.getCategoryId());
        dto.setItemCondition(listing.getItemCondition());
        dto.setDescription(listing.getDescription());
        dto.setTradeType(listing.getTradeType());
        dto.setRegionId(listing.getRegionId());
        dto.setSafePayYn(listing.getSafePayYn());
        return dto;
    }

    private void updateListingStatus(Long listingId, OrderStatus orderStatus) {
        listingRepository.findById(listingId).ifPresent(listing -> {
            switch (orderStatus) {
                case CREATED, PAID -> listing.setStatus("RESERVED");
                case COMPLETED -> listing.setStatus("SOLD");
                case CANCELLED -> listing.setStatus("ACTIVE");
                default -> throw new IllegalStateException("예상하지 못한 상태 전환: " + orderStatus);
            }
            listingRepository.save(listing);
        });
    }

    @Override
    public List<OrderDto> getSellOrders(Long sellerId) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getBuyOrders(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDto getOrder(Long userId, Long orderId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderId));

        // 2. 사용자가 주문의 구매자 또는 판매자일 때만 조회 가능하도록 검증
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        // 3. DTO 변환 및 반환
        return toDto(order);
    }
}

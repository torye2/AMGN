package amgn.amu.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import amgn.amu.domain.User;
import amgn.amu.dto.*;
import amgn.amu.entity.*;
import amgn.amu.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import amgn.amu.dto.OrderDto.OrderStatus;
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
    private final UserRepository userRepository;

    // PG사 DI
    private final KgpPaymentGateway kgpPaymentGateway;
    private final TossPaymentGateway tossPaymentGateway;
    private final KakaoPayGateway kakaoPayGateway;

    @Override
    public boolean isListingInTransaction(Long listingId) {
        return orderRepository.existsByListingIdAndStatusIn(
                listingId, List.of(OrderStatus.CREATED, OrderStatus.PAID, OrderStatus.IN_TRANSIT, OrderStatus.MEETUP_CONFIRMED)
        );
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
    public OrderDto refundPayment(Long buyerId, Long orderId) {
        Order order = findOrderByIdAndCheckBuyer(orderId, buyerId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("결제되지 않은 주문은 환불할 수 없습니다.");
        }

        String merchantUid = "refund_" + order.getId() + "_" + System.currentTimeMillis();
        String impUidFromPayment = order.getImpUid();

        PaymentRequest refundReq = new PaymentRequest(
                order.getId(),
                order.getFinalPrice(),
                order.getPaymentMethod(),
                "refund_" + order.getId(),
                LocalDateTime.now().plusHours(1),
                merchantUid,
                impUidFromPayment,
                null // PG 트랜잭션 ID는 아직 없으므로 null
        );

        if (isLocalOrTest()) {
            // 테스트 모드: 실제 PG 호출 없이 콘솔 출력
            System.out.println("[TEST] 환불 처리 시뮬레이션: "
                    + refundReq.amount() + "원, 결제 수단: " + refundReq.method());
        } else {
            PaymentGateway gateway = selectGateway(refundReq.method());
            boolean refunded = gateway.refund(refundReq);
            if (!refunded) throw new RuntimeException("환불 실패");
        }

        paymentLogRepository.save(PaymentLog.builder()
                .idempotencyKey(refundReq.idempotencyKey())
                .orderId(orderId)
                .type(PaymentType.REFUND)
                .status(PaymentStatus.SUCCESS)
                .merchantUid(refundReq.merchantUid())
                .impUid(refundReq.impUid())
                .pgTransactionId(refundReq.pgTransactionId()) // null일 수도 있음
                .amount(BigDecimal.valueOf(order.getFinalPrice()))
                .createdAt(LocalDateTime.now())
                .build()
        );

        order.setStatus(OrderStatus.CANCELLED);
        order.setTransferStatus(Order.TransferStatus.FAILED);
        order.setUpdatedAt(LocalDateTime.now());
        order.setRefundedAt(LocalDateTime.now());
        orderRepository.save(order);

        updateListingStatus(order.getListingId(), order.getStatus());

        return toDto(order);
    }



    @Override
    public OrderDto create(Long actorUserId, OrderCreateRequest req) {
        // 1. 상품 조회
        Listing listing = listingRepository.findById(req.listingId())
                .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다: " + req.listingId()));

        // 2. 본인 상품 주문 방지
        if (listing.getSellerId().equals(actorUserId)) {
            throw new RuntimeException("본인 상품은 주문할 수 없습니다.");
        }

        // 3. 이미 거래 중이거나 완료된 상품 확인
        if (orderRepository.existsByListingIdAndStatusIn(
                req.listingId(),
                List.of(OrderStatus.CREATED, OrderStatus.PAID, OrderStatus.IN_TRANSIT, OrderStatus.MEETUP_CONFIRMED))) {
            throw new RuntimeException("이미 거래 중인 상품입니다.");
        } else if (orderRepository.existsByListingIdAndStatusIn(req.listingId(), List.of(OrderStatus.COMPLETED))) {
            throw new RuntimeException("이미 판매가 완료된 상품입니다.");
        }

        // 4. Order 엔티티 생성
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

        // 5. 결제 수단 처리 (기본값 KG_INICIS)
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

        // 6. Buyer 엔티티 세팅 (NPE 방지)
        User buyer = userRepository.findById(actorUserId)
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다: " + actorUserId));
        order.setBuyer(buyer);

        // 7. 저장 및 상품 상태 업데이트
        orderRepository.save(order);
        updateListingStatus(order.getListingId(), order.getStatus());

        // 8. DTO 반환
        return toDto(order);
    }



    // ---------------- 결제 ----------------
    // 결제 처리 로직
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderDto pay(Long buyerId, Long orderId, PaymentRequest req) {
        // 1. 주문 및 구매자 검증
        Order order = findOrderByIdAndCheckBuyer(orderId, buyerId);

        // 2. 상태 및 금액 검증
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new RuntimeException("결제할 수 없는 상태입니다.");
        }
        if (!req.amount().equals(order.getFinalPrice())) {
            throw new RuntimeException("결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        // 3. 중복 결제 체크
        if (isDuplicatePayment(req.idempotencyKey())) {
            throw new RuntimeException("중복 결제입니다.");
        }

        // 4. 결제 처리 (테스트 환경에서만 시뮬레이션)
        PaymentRequest.PaymentMethod paymentMethod = req.method();
        boolean success;
        if (isLocalOrTest()) {
            // 테스트 환경: 결제 시뮬레이션
            success = true;
            System.out.println("[TEST] 결제 시뮬레이션: " + req.amount() + "원, 메서드: " + paymentMethod);
        } else {
            // 실제 결제
            PaymentGateway gateway = selectGateway(paymentMethod);
            success = gateway.pay(req);
        }

        if (!success) {
            throw new RuntimeException("결제 실패");
        }

        // 5. 결제 성공 시 buyer 객체 세팅 (NPE 방지)
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다: " + buyerId));
        order.setBuyer(buyer);

        // 6. 주문 상태 및 결제 수단 업데이트
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(LocalDateTime.now());
        order.setImpUid(req.impUid());               // PG사 결제 고유 ID
        orderRepository.save(order);

        // 7. 결제 로그 기록 (테스트 환경에서도 기록 가능)
        paymentLogRepository.save(PaymentLog.builder()
                .idempotencyKey(req.idempotencyKey())
                .orderId(orderId)
                .type(PaymentType.PAY) // ✅ 결제니까 PAY
                .status(PaymentStatus.SUCCESS)
                .merchantUid(req.merchantUid())
                .impUid(req.impUid())
                .pgTransactionId(req.pgTransactionId()) // 테스트 모드면 null일 수 있음
                .amount(BigDecimal.valueOf(order.getFinalPrice()))
                .createdAt(LocalDateTime.now())
                .build()
        );

        // 8. 상품 상태 업데이트
        updateListingStatus(order.getListingId(), order.getStatus());

        // 9. DTO 변환 후 반환
        return toDto(order);
    }

    private boolean isDuplicatePayment(@NotNull String idempotencyKey) {
        // 테스트/로컬 환경에서는 중복 결제 체크 무시
        if (isLocalOrTest()) return false;

        // null 또는 빈 문자열이면 중복 결제 아님
        if (idempotencyKey.isBlank()) {
            return false;
        }

        // 실제 DB 조회
        return paymentLogRepository.existsByIdempotencyKey(idempotencyKey);
    }

    // 로컬/테스트 환경 체크
    private boolean isLocalOrTest() {
        return "local".equals(System.getProperty("spring.profiles.active"))
                || "test".equals(System.getProperty("spring.profiles.active"));
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


        // 2. 상태 변경
        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());

        // 3. 판매자 정산 처리 (PG사 송금 API 연동 or 내부 로직 시뮬레이션)
        boolean transferred = transferToSeller(order);

        // 문자열 대신 enum 전달
        order.setTransferStatus(transferred ? Order.TransferStatus.SUCCESS : Order.TransferStatus.FAILED);

        orderRepository.save(order);

        // 4. 로그 기록
        paymentLogRepository.save(PaymentLog.builder()
                .idempotencyKey("transfer_" + orderId + "_" + System.currentTimeMillis())
                .orderId(orderId)
                .type(PaymentType.PAY) // ✅ 그냥 pay로 완료
                .status(PaymentStatus.SUCCESS)
                .merchantUid(order.getImpUid()) // 임시로 impUid 넣거나 별도 값 세팅
                .impUid(order.getImpUid())
                .pgTransactionId(null) // 송금은 PG 트랜잭션 ID 없을 수 있음
                .amount(BigDecimal.valueOf(order.getFinalPrice()))
                .createdAt(LocalDateTime.now())
                .build()
        );




        updateListingStatus(order.getListingId(), order.getStatus());

        return toDto(order);
    }

    private boolean transferToSeller(Order order) {
        // TODO: PG사 정산 API 연동
        // ex) tossPayments.transfer(order.getSellerId(), order.getFinalPrice());
        System.out.println("[정산] 판매자 " + order.getSellerId() + " 에게 " + order.getFinalPrice() + "원 송금");
        return true; // 테스트 환경에서는 항상 성공으로 처리
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDto cancel(Long actorUserId, Long orderId) {
        System.out.println("[Service] cancel 호출: orderId=" + orderId + ", actorUserId=" + actorUserId);

        Order order = findOrderById(orderId);
        if (!order.getBuyerId().equals(actorUserId)) throw new RuntimeException("권한이 없습니다.");
        if (order.getStatus() == OrderStatus.COMPLETED) throw new RuntimeException("이미 완료된 주문은 취소할 수 없습니다.");

        if (order.getStatus() == OrderStatus.PAID) {
            String merchantUid = "refund_" + orderId + "_" + System.currentTimeMillis();

            PaymentRequest refundReq = new PaymentRequest(
                    order.getId(),
                    order.getFinalPrice(),
                    order.getPaymentMethod(),
                    merchantUid,
                    LocalDateTime.now().plusHours(1),
                    merchantUid,
                    order.getImpUid(),
                    null
            );

            // 환불 로그 기록 (PENDING)
            paymentLogRepository.save(PaymentLog.builder()
                    .idempotencyKey(refundReq.idempotencyKey())
                    .orderId(orderId)
                    .type(PaymentType.REFUND)
                    .status(PaymentStatus.PENDING)
                    .merchantUid(refundReq.merchantUid())
                    .impUid(refundReq.impUid())
                    .amount(BigDecimal.valueOf(order.getFinalPrice()))
                    .createdAt(LocalDateTime.now())
                    .build()
            );

            // 테스트 모드: 해당 결제 정보만 출력
            if (isLocalOrTest()) {
                System.out.println("[TEST] 환불 시뮬레이션");
                System.out.println("OrderId: " + order.getId());
                System.out.println("BuyerId: " + order.getBuyerId());
                System.out.println("Amount: " + order.getFinalPrice());
                System.out.println("PaymentMethod: " + order.getPaymentMethod());
                System.out.println("ImpUid: " + order.getImpUid());
            }

            // 환불 성공 로그 기록
            paymentLogRepository.save(PaymentLog.builder()
                    .idempotencyKey(refundReq.idempotencyKey())
                    .orderId(orderId)
                    .type(PaymentType.REFUND)
                    .status(PaymentStatus.SUCCESS)
                    .merchantUid(refundReq.merchantUid())
                    .impUid(refundReq.impUid())
                    .pgTransactionId(null)
                    .amount(BigDecimal.valueOf(order.getFinalPrice()))
                    .createdAt(LocalDateTime.now())
                    .build()
            );

            order.setRefundedAt(LocalDateTime.now());
            System.out.println("[ALERT] 결제 취소 완료: OrderId=" + order.getId());
        }


        // 주문 상태 변경
        order.setStatus(OrderStatus.CREATED);
        order.setTransferStatus(Order.TransferStatus.FAILED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        updateListingStatus(order.getListingId(), order.getStatus());

        System.out.println("[Service] 주문 상태 변경 완료: " + order.getStatus());
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

    @Transactional
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문 없음"));
        if (!order.getBuyerId().equals(userId)) throw new RuntimeException("권한 없음");

        order.setStatus(OrderStatus.valueOf("DELETED")); // 실제 삭제는 하지 않음
        orderRepository.save(order);
    }

    @Override
    public OrderDto revertCancel(Long userId, Long orderId) {
        Order order = findOrderById(orderId);
        if (!order.getBuyerId().equals(userId)) throw new RuntimeException("권한이 없습니다.");
        if (order.getStatus() != OrderStatus.CREATED) throw new RuntimeException("주문에 오류가 생겨 복원할 수 없습니다.");
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
        // Listing 제목 안전하게 가져오기
        String title = listingRepository.findById(order.getListingId())
                .map(Listing::getTitle)
                .orElse("-");

        // Buyer 엔티티 안전하게 가져오기
        String buyerNickName = "-";
        if (order.getBuyer() != null) {
            buyerNickName = order.getBuyer().getNickName();
        } else {
            // Lazy Loading 안된 경우 직접 조회
            buyerNickName = userRepository.findById(order.getBuyerId())
                    .map(User::getNickName)
                    .orElse("-");
        }

        List<PaymentLog> logs = paymentLogRepository.findByOrderId(order.getId());

        return new OrderDto(
                order.getId(),
                order.getListingId(),
                order.getBuyerId(),
                buyerNickName,
                order.getSellerId(),
                order.getFinalPrice(),
                order.getMethod(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getCreatedAt(),
                title,
                order.getTransferStatus() != null ? order.getTransferStatus().name() : null,
                logs.stream()
                        .map(log -> new PaymentLogDto(
                                log.getType() != null ? log.getType().name() : "-",
                                log.getStatus() != null ? log.getStatus().name() : "-",
                                log.getAmount(),
                                log.getCreatedAt()
                        ))
                        .collect(Collectors.toList())
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
                case CANCELLED, CANCEL_B_S -> listing.setStatus("ACTIVE");
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDto cancelBySeller(Long sellerId, Long orderId) {
        Order order = findOrderById(orderId);

        if (!order.getSellerId().equals(sellerId)) {
            throw new RuntimeException("권한이 없습니다.");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("이미 완료된 주문은 취소할 수 없습니다.");
        }

        // 결제된 주문이면 환불 처리
        if (order.getStatus() == OrderStatus.PAID) {
            String merchantUid = "refund_" + orderId + "_" + System.currentTimeMillis();

            PaymentRequest refundReq = new PaymentRequest(
                    order.getId(),
                    order.getFinalPrice(),
                    order.getPaymentMethod(),
                    merchantUid,
                    LocalDateTime.now().plusHours(1),
                    merchantUid,
                    order.getImpUid(),
                    null
            );

            paymentLogRepository.save(PaymentLog.builder()
                    .idempotencyKey(refundReq.idempotencyKey())
                    .orderId(orderId)
                    .type(PaymentType.REFUND)
                    .status(PaymentStatus.SUCCESS)
                    .merchantUid(refundReq.merchantUid())
                    .impUid(refundReq.impUid())
                    .pgTransactionId(null)
                    .amount(BigDecimal.valueOf(order.getFinalPrice()))
                    .createdAt(LocalDateTime.now())
                    .build()
            );

            order.setRefundedAt(LocalDateTime.now());
        }

        order.setStatus(OrderStatus.CANCEL_B_S);
        order.setTransferStatus(Order.TransferStatus.FAILED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        updateListingStatus(order.getListingId(), order.getStatus());

        return toDto(order);
    }
}

package amgn.amu.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
import amgn.amu.repository.ListingRepository;
import amgn.amu.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;
    
    @Override
    public boolean isListingInTransaction(Long listingId) {
        // CREATED 또는 IN_TRANSIT 상태인 주문이 있으면 true
        return orderRepository.existsByListingIdAndStatusIn(listingId, List.of(OrderStatus.CREATED, OrderStatus.IN_TRANSIT));
    }
    
    @Override
    public OrderDto create(Long actorUserId, OrderCreateRequest req) {
        // 1. listing 존재 여부 확인
        Listing listing = listingRepository.findById(req.listingId())
                .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다: " + req.listingId()));

        // 2. 본인 상품 주문 차단
        if (listing.getSellerId().equals(actorUserId)) {
            throw new RuntimeException("본인 상품은 주문할 수 없습니다.");
        }

        // 3. 이미 거래 중인 주문 있는지 확인
        if (orderRepository.existsByListingIdAndStatusIn(
                req.listingId(),
                List.of(OrderStatus.CREATED, OrderStatus.PAID, OrderStatus.IN_TRANSIT, OrderStatus.MEETUP_CONFIRMED)
        )) {
            throw new RuntimeException("이미 거래 중인 상품입니다.");
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

        // 🚚 배송 관련 정보 매핑
        order.setReceiverName(req.recvName());
        order.setReceiverPhone(req.recvPhone());
        order.setReceiverAddress1(req.recvAddr1());
        order.setReceiverAddress2(req.recvAddr2());
        order.setReceiverZip(req.recvZip());

        // 🤝 직거래 관련 정보 매핑 (추후 필요시 주석 해제)
        // order.setMeetupTime(req.meetupTime());
        // order.setMeetupPlace(req.meetupPlace());

        // 5. 저장
        orderRepository.save(order);

        // 6. DTO 변환 후 반환
        return toDto(order);
    }



    @Override
    public OrderDto pay(Long buyerId, Long orderId, PaymentRequest req) {
        Order order = findOrderByIdAndCheckBuyer(orderId, buyerId);

        // 결제 가능한 상태인지 확인
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new RuntimeException("결제할 수 없는 상태입니다.");
        }

        order.setStatus(OrderStatus.PAID); // 판매 완료
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return toDto(order);
    }

    @Override
    public OrderDto confirmMeetup(Long actorUserId, Long orderId) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderDto.OrderStatus.MEETUP_CONFIRMED);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto inputTracking(Long sellerId, Long orderId, TrackingInputRequest r) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderDto.OrderStatus.IN_TRANSIT);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto confirmDelivered(Long buyerId, Long orderId) {
        Order order = findOrderByIdAndCheckBuyer(orderId, buyerId);
        order.setStatus(OrderDto.OrderStatus.DELIVERED);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto complete(Long actorUserId, Long orderId) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderDto.OrderStatus.COMPLETED);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto cancel(Long actorUserId, Long orderId) {
        Order order = findOrderById(orderId);

        // 결제 완료 상태(COMPLETED)는 취소 불가
        if(order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("이미 판매 완료된 주문은 취소할 수 없습니다.");
        }

        // 상태를 CREATED로 되돌림
        order.setStatus(OrderStatus.CREATED);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public OrderDto dispute(Long actorUserId, Long orderId, String reason) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderDto.OrderStatus.DISPUTED);
        orderRepository.save(order);
        return toDto(order);
    }

    @Override
    public List<OrderDto> myOrders(Long userId) {
        List<Order> orders = orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId);
        return orders.stream()
        		.filter(o -> o.getStatus() != OrderStatus.CANCELLED) // 취소 주문 제외
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ListingDto getListingInfo(Long listingId) {
        return listingRepository.findById(listingId)
                .map(this::toListingDto)
                .orElse(null); // 없으면 null 반환, 컨트롤러에서 404 처리
    }

    // ------------------ 헬퍼 메서드 ------------------
    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문이 존재하지 않습니다: " + orderId));
    }

    private Order findOrderByIdAndCheckBuyer(Long orderId, Long buyerId) {
        Order order = findOrderById(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("권한이 없습니다.");
        }
        return order;
    }

    private OrderDto toDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getListingId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getFinalPrice(),
                order.getMethod(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
    
    @Override
    @Transactional
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문이 존재하지 않습니다: " + orderId));

        // 권한 체크 (구매자만 삭제 가능)
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        // 실제 삭제
        orderRepository.delete(order);
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
    
    
    
    
}

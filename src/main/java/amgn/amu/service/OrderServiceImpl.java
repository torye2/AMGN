package amgn.amu.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import amgn.amu.dto.ListingDto;
import amgn.amu.dto.OrderCreateRequest;
import amgn.amu.dto.OrderDto;
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
    public OrderDto create(Long actorUserId, OrderCreateRequest req) {
        // 1. listing 존재 여부 확인 및 가격 가져오기
        Listing listing = listingRepository.findById(req.listingId())
                .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다: " + req.listingId()));

        // 2. Order 엔티티 생성
        Order order = new Order();
        order.setBuyerId(actorUserId);
        order.setListingId(req.listingId());
        order.setSellerId(listing.getSellerId());   // sellerId도 세팅
        order.setFinalPrice(listing.getPrice().longValue());    // finalPrice를 listing 가격으로 세팅
        order.setMethod(req.method());
        order.setStatus(OrderDto.OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 3. 저장
        orderRepository.save(order);

        // 4. DTO 변환 후 반환
        return toDto(order);
    }


    @Override
    public OrderDto pay(Long buyerId, Long orderId, PaymentRequest req) {
        Order order = findOrderByIdAndCheckBuyer(orderId, buyerId);
        order.setStatus(OrderDto.OrderStatus.PAID);
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
        order.setStatus(OrderDto.OrderStatus.CANCELLED);
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

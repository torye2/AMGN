package amgn.amu.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import amgn.amu.dto.ListingDto;
import amgn.amu.dto.OrderCreateRequest;
import amgn.amu.dto.OrderDto;
import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.TrackingInputRequest;
import amgn.amu.service.OrderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 주문 생성
    @PostMapping
    public OrderDto create(@RequestParam(name="userId") Long userId,
                           @Valid @RequestBody OrderCreateRequest req) {
        return orderService.create(userId, req);
    }

    // 주문 결제
    @PostMapping("/{id}/pay")
    public OrderDto pay(@RequestParam(name="userId") Long userId,
                        @PathVariable(name="id") Long orderId,
                        @RequestBody PaymentRequest req) {
        return orderService.pay(userId, orderId, req);
    }
    // 직거래 확인
    @PostMapping("/{id}/confirm-meetup")
    public OrderDto confirmMeetup(@RequestParam(name="userId") Long userId,
                                  @PathVariable(name="id") Long orderId) {
        return orderService.confirmMeetup(userId, orderId);
    }

    // 배송 입력
    @PostMapping("/{id}/tracking")
    public String inputTracking(@RequestParam(name="userId") Long userId,
                                @PathVariable(name="id") Long orderId,
                                @Valid @RequestBody TrackingInputRequest req) {
        return orderService.inputTracking(userId, orderId, req).toString();
    }

    // 배송 완료 확인
    @PostMapping("/{id}/confirm-delivery")
    public OrderDto delivered(@RequestParam(name="userId") Long userId,
                              @PathVariable(name="id") Long orderId) {
        return orderService.confirmDelivered(userId, orderId);
    }

    // 주문 완료
    @PostMapping("/{id}/complete")
    public OrderDto complete(@RequestParam(name="userId") Long userId,
                             @PathVariable(name="id") Long orderId) {
        return orderService.complete(userId, orderId);
    }

    // 주문 취소
    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable("orderId") Long orderId,
                                       @RequestParam("userId") Long userId) {
        orderService.deleteOrder(userId, orderId); // cancel -> deleteOrder
        return ResponseEntity.ok().build();
    }

    // 분쟁
    @PostMapping("/{id}/dispute")
    public OrderDto dispute(@RequestParam(name="userId") Long userId,
                            @PathVariable(name="id") Long orderId,
                            @RequestParam(name="reason") String reason) {
        return orderService.dispute(userId, orderId, reason);
    }

    // 내 주문 조회
    @GetMapping
    public ResponseEntity<List<OrderDto>> getOrders(@RequestParam(name="userId") Long userId) {
        List<OrderDto> orders = orderService.myOrders(userId);
        return ResponseEntity.ok(orders);
    }

    // 특정 상품 조회
    @GetMapping("/listing/{listingId}")
    public ResponseEntity<ListingDto> getListing(@PathVariable(name="listingId") Long listingId) {
        ListingDto listing = orderService.getListingInfo(listingId);
        return ResponseEntity.ok(listing);
    }
    
    // 특정 listing 거래중인지 체크
    @GetMapping("/check")
    public Map<String, Boolean> checkOrder(@RequestParam Long listingId) {
        boolean exists = orderService.isListingInTransaction(listingId);
        return Map.of("exists", exists);
    }

}

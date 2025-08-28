package amgn.amu.controller;

import amgn.amu.dto.ListingDto;
import amgn.amu.dto.OrderCreateRequest;
import amgn.amu.dto.OrderDto;
import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.TrackingInputRequest;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 세션에서 로그인 사용자 ID 추출
    private Long getUserIdFromSession(HttpSession session) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        return loginUser.getUserId();
    }

    // 주문 생성
    @PostMapping
    public OrderDto create(@Valid @RequestBody OrderCreateRequest req, HttpSession session) {
        Long userId = getUserIdFromSession(session);
        return orderService.create(userId, req);
    }

    // 주문 결제
    @PostMapping("/{orderId}/pay")
    public OrderDto pay(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody PaymentRequest req,
            HttpSession session) {
        Long userId = getUserIdFromSession(session);
        return orderService.pay(userId, orderId, req);
    }

    // 직거래 확인
    @PostMapping("/{orderId}/confirm-meetup")
    public OrderDto confirmMeetup(
            @PathVariable("orderId") Long orderId,
            HttpSession session) {
        Long userId = getUserIdFromSession(session);
        return orderService.confirmMeetup(userId, orderId);
    }

    // 배송 입력
    @PostMapping("/{orderId}/tracking")
    public String inputTracking(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody TrackingInputRequest req,
            HttpSession session) {
        Long userId = getUserIdFromSession(session);
        return orderService.inputTracking(userId, orderId, req).toString();
    }

    // 배송 완료 확인
    @PostMapping("/{orderId}/confirm-delivery")
    public OrderDto delivered(
            @PathVariable("orderId") Long orderId,
            HttpSession session) {
        Long userId = getUserIdFromSession(session);
        return orderService.confirmDelivered(userId, orderId);
    }

    // 주문 완료
    @PostMapping("/{orderId}/complete")
    public OrderDto complete(
            @PathVariable("orderId") Long orderId,
            HttpSession session) {
        Long userId = getUserIdFromSession(session);
        return orderService.complete(userId, orderId);
    }

    // 주문 취소
    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable("orderId") Long orderId,
            HttpSession session) {
        Long userId = getUserIdFromSession(session);
        orderService.deleteOrder(userId, orderId);
        return ResponseEntity.ok().build();
    }

    // 분쟁
    @PostMapping("/{orderId}/dispute")
    public OrderDto dispute(
            @PathVariable("orderId") Long orderId,
            @RequestParam("reason") String reason,
            HttpSession session) {
        Long userId = getUserIdFromSession(session);
        return orderService.dispute(userId, orderId, reason);
    }

    // 내 주문 조회
    @GetMapping
    public ResponseEntity<List<OrderDto>> getOrders(HttpSession session) {
        Long userId = getUserIdFromSession(session);
        List<OrderDto> orders = orderService.myOrders(userId);
        return ResponseEntity.ok(orders);
    }

    // 특정 상품 조회
    @GetMapping("/listing/{listingId}")
    public ResponseEntity<ListingDto> getListing(
            @PathVariable("listingId") Long listingId) {
        ListingDto listing = orderService.getListingInfo(listingId);
        return ResponseEntity.ok(listing);
    }

    // 특정 listing 거래중인지 체크
    @GetMapping("/check")
    public Map<String, Boolean> checkOrder(@RequestParam("listingId") Long listingId) {
        boolean exists = orderService.isListingInTransaction(listingId);
        return Map.of("exists", exists);
    }
}

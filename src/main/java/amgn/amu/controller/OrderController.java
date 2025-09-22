package amgn.amu.controller;

import amgn.amu.dto.*;
import amgn.amu.service.OrderService;
import amgn.amu.service.PaymentService;
import amgn.amu.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;
	private final PaymentService paymentService;
	private final ReviewService reviewService;

	// ---------------- 공통 유틸 ----------------
	private Long getUserIdFromSession(HttpSession session) {
		LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
		if (loginUser == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		return loginUser.getUserId();
	}

	// ---------------- 주문 ----------------

	@PostMapping
	public OrderDto create(@Valid @RequestBody OrderCreateRequest req, HttpSession session) {
		return orderService.create(getUserIdFromSession(session), req);
	}

	@PostMapping("/{orderId}/pay")
	public OrderDto payDefault(@PathVariable Long orderId,
							   @RequestBody PaymentRequest req,
							   HttpSession session) {
		// 기본 결제 처리 (단일 PG용)
		return orderService.pay(getUserIdFromSession(session), orderId, req);
	}

	@PostMapping("/{orderId}/pay/kakao")
	public OrderDto payKakao(@PathVariable Long orderId,
							 @RequestBody PaymentRequest req,
							 HttpSession session) {
		return paymentService.payWithKakao(getUserIdFromSession(session), orderId, req);
	}

	@PostMapping("/{orderId}/pay/inicis")
	public OrderDto payInicis(@PathVariable Long orderId,
							  @RequestBody PaymentRequest req,
							  HttpSession session) {
		return paymentService.payWithInicis(getUserIdFromSession(session), orderId, req);
	}

	@PostMapping("/{orderId}/confirm-meetup")
	public OrderDto confirmMeetup(@PathVariable Long orderId, HttpSession session) {
		return orderService.confirmMeetup(getUserIdFromSession(session), orderId);
	}

	@PostMapping("/{orderId}/tracking")
	public OrderDto inputTracking(@PathVariable Long orderId,
								  @Valid @RequestBody TrackingInputRequest req,
								  HttpSession session) {
		return orderService.inputTracking(getUserIdFromSession(session), orderId, req);
	}

	@PostMapping("/{orderId}/confirm-delivery")
	public OrderDto delivered(@PathVariable Long orderId, HttpSession session) {
		return orderService.confirmDelivered(getUserIdFromSession(session), orderId);
	}

	@PostMapping("/{orderId}/complete")
	public OrderDto complete(@PathVariable Long orderId, HttpSession session) {
		return orderService.complete(getUserIdFromSession(session), orderId);
	}

	@DeleteMapping("/{orderId}/cancel")
	public ResponseEntity<Void> cancel(@PathVariable Long orderId, HttpSession session) {
		orderService.cancel(getUserIdFromSession(session), orderId);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{orderId}/dispute")
	public OrderDto dispute(@PathVariable Long orderId,
							@RequestParam String reason,
							HttpSession session) {
		return orderService.dispute(getUserIdFromSession(session), orderId, reason);
	}

	@GetMapping
	public ResponseEntity<List<OrderDto>> getOrders(HttpSession session) {
		return ResponseEntity.ok(orderService.myOrders(getUserIdFromSession(session)));
	}

	@GetMapping("/sell")
	public ResponseEntity<List<OrderDto>> getSellOrders(HttpSession session) {
		return ResponseEntity.ok(orderService.getSellOrders(getUserIdFromSession(session)));
	}

	@GetMapping("/buy")
	public ResponseEntity<List<OrderDto>> getBuyOrders(HttpSession session) {
		return ResponseEntity.ok(orderService.getBuyOrders(getUserIdFromSession(session)));
	}

	@PostMapping("/{orderId}/revert")
	public ResponseEntity<OrderDto> revertCancel(@PathVariable Long orderId, HttpSession session) {
		return ResponseEntity.ok(orderService.revertCancel(getUserIdFromSession(session), orderId));
	}

	// ---------------- 리뷰 ----------------

	@GetMapping("/reviews/orders/{orderId}")
	public ResponseEntity<List<ReviewDto>> getReviewsByOrder(@PathVariable Long orderId) {
		return ResponseEntity.ok(reviewService.getReviewsByOrder(orderId));
	}

	@PostMapping("/reviews")
	public ResponseEntity<Void> createReview(@RequestBody Map<String, Object> payload, HttpSession session) {
		reviewService.createReview(getUserIdFromSession(session), payload);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/reviews/{reviewId}")
	public ResponseEntity<Void> updateReview(@PathVariable Long reviewId,
											 @RequestBody Map<String, Object> payload,
											 HttpSession session) {
		reviewService.updateReview(getUserIdFromSession(session), reviewId, payload);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/reviews/{reviewId}")
	public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId, HttpSession session) {
		reviewService.deleteReview(getUserIdFromSession(session), reviewId);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/pre-register/{orderId}")
	public PaymentResponse preRegister(@PathVariable Long orderId, HttpSession session) {
		Long userId = getUserIdFromSession(session);
		PaymentRequest req = new PaymentRequest(orderId, orderService.getOrder(userId, orderId).finalPrice(),
				PaymentRequest.PaymentMethod.KG_INICIS, "order_" + orderId + "_" + System.currentTimeMillis(), LocalDateTime.now().plusMinutes(30));
		return paymentService.preparePayment(userId, req);
	}

}

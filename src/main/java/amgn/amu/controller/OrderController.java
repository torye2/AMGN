package amgn.amu.controller;

import amgn.amu.dto.*;
import amgn.amu.service.OrderService;
import amgn.amu.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;
	private final ReviewService reviewService;

	private Long getUserIdFromSession(HttpSession session) {
		LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
		if (loginUser == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		return loginUser.getUserId();
	}

	// ---------------- 주문 ----------------

	@PostMapping
	public OrderDto create(@RequestBody OrderCreateRequest req, HttpSession session) {
		Long userId = getUserIdFromSession(session);
		return orderService.create(userId, req);
	}

	@PostMapping("/{orderId}/pay")
	public OrderDto pay(@PathVariable Long orderId, @RequestBody PaymentRequest req, HttpSession session) {
		Long userId = getUserIdFromSession(session);
		return orderService.pay(userId, orderId, req);
	}

	@PostMapping("/{orderId}/complete")
	public OrderDto complete(@PathVariable Long orderId, HttpSession session) {
		Long userId = getUserIdFromSession(session);
		return orderService.complete(userId, orderId);
	}

	@DeleteMapping("/{orderId}/cancel")
	public ResponseEntity<Void> cancel(@PathVariable Long orderId, HttpSession session) {
		Long userId = getUserIdFromSession(session);
		orderService.deleteOrder(userId, orderId);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/buy")
	public ResponseEntity<List<OrderDto>> getBuyOrders(HttpSession session) {
		try {
			Long userId = getUserIdFromSession(session);
			System.out.println("getBuyOrders userId: " + userId);
			List<OrderDto> buyOrders = orderService.getBuyOrders(userId);
			System.out.println("buyOrders size: " + buyOrders.size());
			return ResponseEntity.ok(buyOrders);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// 새로 추가된 /sell API
	@GetMapping("/sell")
	public ResponseEntity<List<OrderDto>> getSellOrders(HttpSession session) {
		try {
			Long userId = getUserIdFromSession(session);
			System.out.println("getSellOrders userId: " + userId);
			List<OrderDto> sellOrders = orderService.getSellOrders(userId); // 서비스에 getSellOrders 메서드 필요
			System.out.println("sellOrders size: " + sellOrders.size());
			return ResponseEntity.ok(sellOrders);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// ---------------- 리뷰 ----------------

	@GetMapping("/reviews/orders/{orderId}")
	public ResponseEntity<List<ReviewDto>> getReviewsByOrder(@PathVariable Long orderId) {
		return ResponseEntity.ok(reviewService.getReviewsByOrder(orderId));
	}

	@PostMapping("/reviews")
	public ResponseEntity<Void> createReview(@RequestBody Map<String, Object> payload, HttpSession session) {
		Long userId = getUserIdFromSession(session);
		reviewService.createReview(userId, payload);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/reviews/{reviewId}")
	public ResponseEntity<Void> updateReview(@PathVariable Long reviewId,
											 @RequestBody Map<String, Object> payload,
											 HttpSession session) {
		Long userId = getUserIdFromSession(session);
		reviewService.updateReview(userId, reviewId, payload);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/reviews/{reviewId}")
	public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId, HttpSession session) {
		Long userId = getUserIdFromSession(session);
		reviewService.deleteReview(userId, reviewId);
		return ResponseEntity.ok().build();
	}
}

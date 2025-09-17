package amgn.amu.controller;

import java.util.List;
import java.util.Map;

import amgn.amu.dto.ReviewDto;
import amgn.amu.dto.ReviewOrderDto;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    private Long getUserId(HttpSession session) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) throw new RuntimeException("로그인이 필요합니다.");
        return loginUser.getUserId();
    }

    // 내 리뷰 작성 가능한 주문
    @GetMapping("/mine")
    public ResponseEntity<List<ReviewOrderDto>> myOrders(HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(reviewService.getReviewableOrders(userId));
    }

    // 특정 주문 리뷰 리스트
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable Long orderId) {
        return ResponseEntity.ok(reviewService.getReviewsByOrder(orderId));
    }

    // 리뷰 작성
    @PostMapping
    public ResponseEntity<Void> createReview(@RequestBody Map<String, Object> payload, HttpSession session) {
        Long userId = getUserId(session);
        reviewService.createReview(userId, payload);
        return ResponseEntity.ok().build();
    }

    // 판매자가 받은 후기 전체 조회 (로그인한 본인)
    @GetMapping("/received")
    public ResponseEntity<List<ReviewDto>> receivedReviews(HttpSession session) {
        Long sellerId = getUserId(session);
        return ResponseEntity.ok(reviewService.getReviewsBySeller(sellerId));
    }

    // 특정 sellerId의 상점후기 조회 (상점 페이지 용)
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ReviewDto>> getSellerReviews(@PathVariable Long sellerId) {
        return ResponseEntity.ok(reviewService.getReviewsBySeller(sellerId));
    }

}

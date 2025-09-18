package amgn.amu.controller;

import java.util.List;
import java.util.Map;

import amgn.amu.dto.ReviewDto;
import amgn.amu.dto.ReviewOrderDto;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.SellerReviewsSummaryDto;
import amgn.amu.service.ProductReviewService;
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
    private final ProductReviewService productReviewService;

    private Long getUserId(HttpSession session) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) throw new RuntimeException("로그인이 필요합니다.");
        return loginUser.getUserId();
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ReviewOrderDto>> myOrders(HttpSession session) {
        Long userId = getUserId(session);
        return ResponseEntity.ok(reviewService.getReviewableOrders(userId));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable Long orderId) {
        return ResponseEntity.ok(reviewService.getReviewsByOrder(orderId));
    }

    @PostMapping
    public ResponseEntity<Void> createReview(@RequestBody Map<String, Object> payload, HttpSession session) {
        Long userId = getUserId(session);
        reviewService.createReview(userId, payload);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(@PathVariable Long reviewId,
                                             @RequestBody Map<String, Object> payload,
                                             HttpSession session) {
        Long userId = getUserId(session);
        reviewService.updateReview(userId, reviewId, payload);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId, HttpSession session) {
        Long userId = getUserId(session);
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/received")
    public ResponseEntity<List<ReviewDto>> receivedReviews(HttpSession session) {
        Long sellerId = getUserId(session);
        return ResponseEntity.ok(reviewService.getReviewsBySeller(sellerId));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ReviewDto>> getSellerReviews(@PathVariable Long sellerId) {
        return ResponseEntity.ok(reviewService.getReviewsBySeller(sellerId));
    }

    @GetMapping("/seller/{sellerId}/summary")
    public ResponseEntity<SellerReviewsSummaryDto> getSellerReviewsSummary(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "3") int limit,
            @RequestParam(required = false) Long listingId
    ) {
        return ResponseEntity.ok(productReviewService.getSellerReviewsSummary(sellerId, limit, listingId));
    }
}

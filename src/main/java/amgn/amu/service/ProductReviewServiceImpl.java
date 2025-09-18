package amgn.amu.service;

import amgn.amu.dto.*;
import amgn.amu.entity.Order;
import amgn.amu.entity.Review;
import amgn.amu.repository.OrderRepository;
import amgn.amu.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewOrderDto> getReviewableOrders(Long userId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(order -> !order.isReviewed())
                .map(order -> new ReviewOrderDto(order.getId(), order.getListing().getTitle()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByOrder(Long orderId) {
        return reviewRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(r -> new ReviewDto(
                        r.getId(),
                        r.getOrder().getId(),
                        r.getRaterId(),
                        r.getScore(),
                        r.getRvComment(),
                        r.getCreatedAt(),
                        r.getOrder().getListing().getTitle()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void createReview(Long raterId, Map<String, Object> payload) {
        Long orderId = ((Number) payload.get("orderId")).longValue();
        Integer score = ((Number) payload.get("score")).intValue();
        String rvComment = (String) payload.getOrDefault("rvComment", "");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));

        if (order.isReviewed()) {
            throw new RuntimeException("이미 리뷰가 작성된 주문입니다.");
        }

        Review review = new Review();
        review.setOrder(order);
        review.setRaterId(raterId);
        review.setRateeId(order.getSeller().getUserId());
        review.setScore(score);
        review.setRvComment(rvComment);
        review.setCreatedAt(LocalDateTime.now());

        reviewRepository.save(review);

        // 선택: 주문에 reviewed 플래그를 쓰고 있다면 true로 마킹
        // order.setReviewed(true);
        // orderRepository.save(order);
    }

    @Override
    public void updateReview(Long userId, Long reviewId, Map<String, Object> payload) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getRaterId().equals(userId)) {
            throw new RuntimeException("리뷰를 수정할 권한이 없습니다.");
        }

        if (payload.containsKey("score")) {
            review.setScore(((Number) payload.get("score")).intValue());
        }
        if (payload.containsKey("rvComment")) {
            review.setRvComment((String) payload.get("rvComment"));
        }

        reviewRepository.save(review);
    }

    @Override
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getRaterId().equals(userId)) {
            throw new RuntimeException("리뷰를 삭제할 권한이 없습니다.");
        }

        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsBySeller(Long sellerId) {
        return reviewRepository.findByRateeIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(r -> new ReviewDto(
                        r.getId(),
                        r.getOrder().getId(),
                        r.getRaterId(),
                        r.getScore(),
                        r.getRvComment(),
                        r.getCreatedAt(),
                        r.getOrder().getListing().getTitle()
                ))
                .collect(Collectors.toList());
    }

    // ✅ 요약(평균/개수/최근 N개)
    @Override
    @Transactional(readOnly = true)
    public SellerReviewsSummaryDto getSellerReviewsSummary(Long sellerId, int limit, Long listingId) {
        // 최근 N개
        var pageable = PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Review> recent = reviewRepository.findRecentBySellerAndListing(sellerId, listingId, pageable);

        // 평균/개수
        Object[] agg = reviewRepository.findAvgAndCountBySellerAndListing(sellerId, listingId);
        double avg = ((Number) agg[0]).doubleValue();
        long count = ((Number) agg[1]).longValue();

        // reviewerNickname은 사용자 테이블 조인이 없다면 '익명'으로 처리
        var items = recent.stream()
                .map(r -> new ReviewSummaryItemDto(
                        r.getId(),
                        r.getScore(),
                        r.getRvComment(),
                        "익명",
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new SellerReviewsSummaryDto(avg, count, items);
    }
}

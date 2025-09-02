package amgn.amu.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import amgn.amu.dto.ReviewDto;
import amgn.amu.dto.ReviewOrderDto;
import amgn.amu.entity.Order;
import amgn.amu.entity.Review;
import amgn.amu.repository.OrderRepository;
import amgn.amu.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewOrderDto> getReviewableOrders(Long userId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(order -> !order.isReviewed())
                .map(order -> new ReviewOrderDto(
                        order.getId(),
                        order.getListing().getTitle()
                ))
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
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void createReview(Long userId, Map<String, Object> payload) {
        Long orderId = Long.valueOf(payload.get("orderId").toString());
        Integer score = Integer.valueOf(payload.get("score").toString());
        String rvComment = payload.getOrDefault("rvComment", "").toString();

        if (reviewRepository.existsByOrderIdAndRaterId(orderId, userId)) {
            throw new RuntimeException("이미 리뷰를 작성했습니다.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문이 존재하지 않습니다: " + orderId));

        Review review = new Review();
        review.setOrder(order);
        review.setRaterId(userId);
        review.setScore(score);
        review.setRvComment(rvComment);
        review.setCreatedAt(LocalDateTime.now());

        reviewRepository.save(review);
    }
}

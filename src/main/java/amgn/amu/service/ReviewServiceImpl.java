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
    public void createReview(Long raterId, Map<String, Object> payload) {
        Long orderId = ((Number) payload.get("orderId")).longValue();
        Integer score = ((Number) payload.get("score")).intValue();
        String rvComment = (String) payload.getOrDefault("rvComment", ""); // null이면 공란 처리

        // 주문 가져오기
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다."));


        Review review = new Review();
        review.setOrder(order);
        review.setRaterId(raterId);
        review.setRateeId(order.getSeller().getUserId()); // Long 타입 맞춤
        review.setScore(score);
        review.setRvComment(rvComment); // 빈 문자열 들어가도 OK
        review.setCreatedAt(LocalDateTime.now());

        reviewRepository.save(review);
    }

}

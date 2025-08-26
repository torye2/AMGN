package amgn.amu.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import amgn.amu.dto.ReviewCreateRequest;
import amgn.amu.dto.ReviewDto;
import amgn.amu.entity.Order;
import amgn.amu.entity.Review;
import amgn.amu.repository.OrderRepository;
import amgn.amu.repository.ReviewRepository;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public ReviewDto create(Long raterId, ReviewCreateRequest req) {
        // 주문 조회
        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 구매자만 작성 가능
        if (!raterId.equals(order.getBuyerId())) {
            throw new IllegalStateException("구매자만 리뷰를 작성할 수 있습니다.");
        }

        // 중복 리뷰 방지
        if (reviewRepository.existsByOrderIdAndRaterId(req.orderId(), raterId)) {
            throw new IllegalStateException("이미 리뷰가 존재합니다.");
        }

        // 판매자 ID
        Long rateeId = order.getSellerId();

        // 리뷰 생성
        Review review = new Review();
        review.setOrderId(req.orderId());
        review.setRaterId(raterId);
        review.setRateeId(rateeId);
        review.setScore(req.score());
        review.setRvComment(req.rvComment());
        review.setCreatedAt(java.time.LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        return new ReviewDto(
                saved.getReviewId(),
                saved.getOrderId(),
                saved.getRaterId(),
                saved.getRateeId(),
                saved.getScore(),
                saved.getRvComment(),
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> listForUser(Long rateeId, int limit) {
        return reviewRepository.findByRateeIdOrderByCreatedAtDesc(rateeId)
                .stream()
                .limit(limit)
                .map(rev -> new ReviewDto(
                        rev.getReviewId(),
                        rev.getOrderId(),
                        rev.getRaterId(),
                        rev.getRateeId(),
                        rev.getScore(),
                        rev.getRvComment(),
                        rev.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public double ratingAverage(Long rateeId) {
        List<Review> reviews = reviewRepository.findByRateeIdOrderByCreatedAtDesc(rateeId);
        return reviews.stream()
                      .mapToInt(Review::getScore)
                      .average()
                      .orElse(0.0);
    }
}

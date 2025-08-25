package amgn.amu.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import amgn.amu.dto.ReviewCreateRequest;
import amgn.amu.dto.ReviewDto;
import amgn.amu.entity.Review;
import amgn.amu.repository.ReviewRepository;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public ReviewDto create(Long raterId, ReviewCreateRequest req) {
        // 이미 리뷰가 있는지 체크
        if (reviewRepository.existsByOrderIdAndRaterId(req.orderId(), raterId)) {
            throw new IllegalStateException("이미 리뷰가 존재합니다.");
        }

        // Review 엔티티 생성
        Review review = new Review();
        review.setOrderId(req.orderId());
        review.setRaterId(raterId);
        review.setRateeId(/* 주문에서 받는 사람 ID */ 0L); // 필요 시 주문 조회 후 rateeId 설정 0L => 리터럴 타입 L
        review.setScore(req.score());
        review.setComment(req.comment());
        review.setCreatedAt(java.time.LocalDateTime.now());

        // 저장 후 DTO 변환
        Review saved = reviewRepository.save(review);

        return new ReviewDto(
            saved.getReviewId(),
            saved.getOrderId(),
            saved.getRaterId(),
            saved.getRateeId(),
            saved.getScore(),
            saved.getComment(),
            saved.getCreatedAt()
        );
    }
    // 유저 리뷰 조회
    @Override
    public List<ReviewDto> listForUser(Long rateeId, int limit) {
        return reviewRepository.findByRateeIdOrderByCreatedAtDesc(rateeId)
                .stream()
                .limit(limit)
                .map(r -> new ReviewDto(
                        r.getReviewId(),
                        r.getOrderId(),
                        r.getRaterId(),
                        r.getRateeId(),
                        r.getScore(),
                        r.getComment(),
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
    // 평균 평점
    @Override
    public double ratingAverage(Long rateeId) {
        List<Review> reviews = reviewRepository.findByRateeIdOrderByCreatedAtDesc(rateeId);
        return reviews.stream()
                      .mapToInt(Review::getScore)
                      .average()
                      .orElse(0.0);
    }
}

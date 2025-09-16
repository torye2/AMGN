package amgn.amu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import amgn.amu.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderIdAndRaterId(Long orderId, Long raterId);

    List<Review> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    // 판매자별 리뷰 조회
    List<Review> findByRateeIdOrderByCreatedAtDesc(Long rateeId);
}

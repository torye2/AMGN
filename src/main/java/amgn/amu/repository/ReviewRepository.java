package amgn.amu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import amgn.amu.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 주문 ID + 작성자 ID로 중복 체크
    boolean existsByOrderIdAndRaterId(Long orderId, Long raterId);

    // 특정 유저(rateeId)의 리뷰를 최신순으로 가져오기
    List<Review> findByRateeIdOrderByCreatedAtDesc(Long rateeId);
}
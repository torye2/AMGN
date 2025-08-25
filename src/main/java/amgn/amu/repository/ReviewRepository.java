package amgn.amu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import amgn.amu.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
	  boolean existsByOrderIdAndRaterId(Long orderId, Long raterId);
	  List<Review> findByRateeIdOrderByCreatedAtDesc(Long rateeId);
	}
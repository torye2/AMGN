package amgn.amu.service;

import java.util.List;
import java.util.Map;

import amgn.amu.dto.ReviewDto;
import amgn.amu.dto.ReviewOrderDto;

public interface ReviewService {

    List<ReviewOrderDto> getReviewableOrders(Long userId);

    List<ReviewDto> getReviewsByOrder(Long orderId);

    void createReview(Long userId, Map<String, Object> payload);

    // 판매자가 받은 후기 전체 조회
    List<ReviewDto> getReviewsBySeller(Long sellerId);
}

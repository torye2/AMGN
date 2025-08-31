package amgn.amu.service;

import java.util.List;
import java.util.Map;

import amgn.amu.dto.ReviewDto;
import amgn.amu.dto.ReviewOrderDto;

public interface ReviewService {

    List<ReviewOrderDto> getReviewableOrders(Long userId);

    List<ReviewDto> getReviewsByOrder(Long orderId);

    void createReview(Long userId, Map<String, Object> payload);
}

package amgn.amu.service;

import amgn.amu.dto.ReviewDto;
import amgn.amu.dto.ReviewOrderDto;
import amgn.amu.dto.SellerReviewsSummaryDto;

import java.util.List;
import java.util.Map;

public interface ProductReviewService {

    List<ReviewOrderDto> getReviewableOrders(Long userId);

    List<ReviewDto> getReviewsByOrder(Long orderId);

    void createReview(Long userId, Map<String, Object> payload);

    // 판매자가 받은 후기 전체 조회
    List<ReviewDto> getReviewsBySeller(Long sellerId);

    // 리뷰 수정
    void updateReview(Long userId, Long reviewId, Map<String, Object> payload);

    // 리뷰 삭제
    void deleteReview(Long userId, Long reviewId);

    // ✅ 판매자 후기 요약(평점 + 최근 N개)
    SellerReviewsSummaryDto getSellerReviewsSummary(Long sellerId, int limit, Long listingId);
}

package amgn.amu.repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import amgn.amu.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderIdAndRaterId(Long orderId, Long raterId);

    List<Review> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    // 판매자별 리뷰 조회
    List<Review> findByRateeIdOrderByCreatedAtDesc(Long rateeId);

    // 판매자 기준 최신 N개 (listingId 옵션)
    @Query("""
        select r from Review r
        where r.rateeId = :sellerId
          and (:listingId is null or r.order.listing.id = :listingId)
        order by r.createdAt desc
    """)
    List<Review> findRecentBySellerAndListing(
            @Param("sellerId") Long sellerId,
            @Param("listingId") Long listingId,
            Pageable pageable
    );

    // 판매자 기준 평균/개수 (listingId 옵션)
    @Query("""
        select coalesce(avg(r.score), 0), count(r)
        from Review r
        where r.rateeId = :sellerId
          and (:listingId is null or r.order.listing.id = :listingId)
    """)
    Object[] findAvgAndCountBySellerAndListing(
            @Param("sellerId") Long sellerId,
            @Param("listingId") Long listingId
    );
}

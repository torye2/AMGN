package amgn.amu.repository;

import amgn.amu.entity.ListingWish;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ListingWishRepository extends JpaRepository<ListingWish, Long> {

    // 해당 사용자가 특정 상품을 찜했는지 여부
    boolean existsByListingIdAndUserId(Long listingId, Long userId);

    // 상품별 찜 개수
    long countByListingId(Long listingId);

    // 사용자별 전체 찜 개수 (추가)
    long countByUserId(Long userId);

    // 해당 사용자의 특정 상품 찜 삭제(토글 해제)
    void deleteByListingIdAndUserId(Long listingId, Long userId);

    // 사용자별 찜 전체 목록
    List<ListingWish> findByUserId(Long userId);

    // 상품을 찜한 사용자 목록
    List<ListingWish> findByListingId(Long listingId);

    // (옵션) 최근 찜한 상품 ID들만 빠르게 가져오기
    @Query("select w.listingId from ListingWish w where w.userId = :userId order by w.createdAt desc")
    List<Long> findListingIdsByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}

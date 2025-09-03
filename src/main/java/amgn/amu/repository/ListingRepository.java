package amgn.amu.repository;

import amgn.amu.entity.Listing;
import amgn.amu.entity.ListingAttr;
import amgn.amu.entity.ListingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    // 특정 판매자가 등록한 상품 리스트 조회
    List<Listing> findBySellerId(Long sellerId);

    // 특정 카테고리 상품 조회
    List<Listing> findByCategoryId(Integer categoryId);

    // 가격 범위로 상품 조회
    List<Listing> findByPriceBetween(Long minPrice, Long maxPrice);

    List<Listing> findByCategoryIdAndListingIdNot(Long categoryId, Long listingId);
}
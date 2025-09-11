package amgn.amu.repository;

import amgn.amu.dto.ListingDto;
import amgn.amu.entity.Listing;
import amgn.amu.entity.ListingAttr;
import amgn.amu.entity.ListingPhoto;
//import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;


import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    // 특정 판매자가 등록한 상품 리스트 조회
    List<Listing> findBySellerId(Long sellerId);

    // 특정 카테고리 상품 조회
    List<Listing> findByCategoryId(Integer categoryId);

    // 가격 범위로 상품 조회
    List<Listing> findByPriceBetween(Long minPrice, Long maxPrice);

    List<Listing> findByCategoryIdAndListingIdNot(Long categoryId, Long listingId);

    // ✅ 제목 검색 (ACTIVE + 포함 + 대소문자 무시 + 페이징 + photos 로딩)
    @EntityGraph(attributePaths = {"photos"})
    @Query("""
        SELECT l
        FROM Listing l
        WHERE l.status = 'ACTIVE'
          AND (:title IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :title, '%')))
        """)
    Page<Listing> searchByTitle(@Param("title") String title, Pageable pageable);
}
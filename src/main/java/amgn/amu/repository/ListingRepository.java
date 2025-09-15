package amgn.amu.repository;

import amgn.amu.dto.ListingDto;
import amgn.amu.dto.ListingSummaryResponse;
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
    Page<Listing> findByCategoryIdIn(List<Long> categoryIds, Pageable pageable);
    Page<Listing> findByCategoryIdInAndRegionIdIn(List<Long> categoryIds, List<Long> regionIds, Pageable pageable);
    Page<Listing> findByRegionIdIn(List<Long> regionIds, Pageable pageable);



    // ✅ 제목 검색 (ACTIVE + 포함 + 대소문자 무시 + 페이징 + photos 로딩)
    @EntityGraph(attributePaths = {"photos"})
    @Query("""
        SELECT l
        FROM Listing l
        WHERE l.status = 'ACTIVE'
          AND (:title IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :title, '%')))
        """)
    Page<Listing> searchByTitle(@Param("title") String title, Pageable pageable);

    @Query(
            value = """
    SELECT new amgn.amu.dto.ListingSummaryResponse(
      l.listingId, l.title, l.price, l.currency, l.categoryId, l.regionId,
      COALESCE(MIN(p.url), NULL),
      l.createdAt, l.wishCount, l.viewCount
    )
    FROM Listing l
    LEFT JOIN l.photos p
    WHERE UPPER(TRIM(l.status)) = 'ACTIVE'
      AND (:q IS NULL OR LOWER(FUNCTION('REPLACE', l.title, ' ', ''))
           LIKE LOWER(CONCAT('%', :q, '%')))
    GROUP BY
      l.listingId, l.title, l.price, l.currency,
      l.categoryId, l.regionId, l.createdAt, l.wishCount, l.viewCount
  """,
            countQuery = """
    SELECT COUNT(l)
    FROM Listing l
    WHERE UPPER(TRIM(l.status)) = 'ACTIVE'
      AND (:q IS NULL OR LOWER(FUNCTION('REPLACE', l.title, ' ', ''))
           LIKE LOWER(CONCAT('%', :q, '%')))
  """
    )
    Page<ListingSummaryResponse> searchByTitleSummary(@Param("q") String normalizedTitle, Pageable pageable);


    // 비페이징 (목록 정렬)
    List<Listing> findByRegionIdOrderByListingIdDesc(Long regionId);

    // 페이징
    Page<Listing> findByRegionId(Long regionId, Pageable pageable);

    Page<Listing> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Listing> findByCategoryIdAndRegionId(Long categoryId, Long regionId, Pageable pageable);


    @Query("""
        select l from Listing l
        where (:catPath is null or exists (
            select 1 from Category c
            where c.id = l.categoryId
              and c.path like concat(:catPath, '%')
        ))
          and (:regPath is null or exists (
            select 1 from Region r
            where r.id = l.regionId
              and r.path like concat(:regPath, '%')
        ))
        """)
    Page<Listing> searchByPaths(@Param("catPath") String catPath,
                                @Param("regPath") String regPath,
                                Pageable pageable);
}
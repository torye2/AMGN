// src/main/java/amgn/amu/repository/AwardQueryRepository.java
package amgn.amu.repository;

import amgn.amu.entity.MonthlySellerAward;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AwardQueryRepository extends JpaRepository<MonthlySellerAward, Long> {

    @Query(value =
            "SELECT " +
                    "  msa.seller_id       AS sellerId, " +
                    "  COALESCE(u.nick_name, u.user_name) AS sellerName, " +
                    "  p.profile_img AS avatarUrl, " +
                    "  msa.metric          AS metric, " +
                    "  msa.value_num       AS value, " +                      // 판매수량
                    "  msa.rank_num        AS rankNum " +
                    "FROM `monthly_seller_awards` msa " +
                    "JOIN `USERS` u ON u.user_id = msa.seller_id " +
                    "LEFT JOIN `user_profile` p ON p.user_id = u.user_id " +
                    "WHERE msa.ym = :ymStart " +
                    "  AND msa.metric = :metric " +                           // 'units'
                    "ORDER BY msa.rank_num ASC",
            nativeQuery = true)
    List<TopSellerRow> findTopSellers(
            @Param("ymStart") String ymStart,
            @Param("metric") String metric,
            Pageable pageable
    );

    @Query(value = """
        SELECT seller_id,
               units,
               rank_num
        FROM (
          SELECT l.seller_id AS seller_id,
                 COUNT(*) AS units,
                 ROW_NUMBER() OVER (ORDER BY COUNT(*) DESC, l.seller_id) AS rank_num
          FROM listings l
          WHERE l.status = 'SOLD'
            AND l.updated_at >= (NOW() - INTERVAL 30 DAY)
          GROUP BY l.seller_id
        ) t
        ORDER BY rank_num
        """, nativeQuery = true)
    List<TopSellerRow> findTopSellersRolling(int limit);

}

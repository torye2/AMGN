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
}

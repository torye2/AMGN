package amgn.amu.repository;

import java.math.BigDecimal;

public interface TopSellerRow {
    Integer getSellerId();
    String  getSellerName();
    String  getAvatarUrl();
    String  getMetric();
    BigDecimal getValue();   // == units 저장값
    Integer getRankNum();
}
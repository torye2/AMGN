package amgn.amu.dto;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class SellerOfMonthDto {
    private Integer sellerId;
    private String  sellerName;
    private String  avatarUrl;
    private String  metric;      // "units"
    private Double  value;       // == units (awards.value_num)
    private Integer orderCount;  // 선택
    private Integer units;       // 보기 좋게 int로
}
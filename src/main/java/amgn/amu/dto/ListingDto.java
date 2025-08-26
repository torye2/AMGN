package amgn.amu.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
public class ListingDto {
    private String title;
    private BigDecimal price; // DECIMAL(12, 0)과 매핑되도록 BigDecimal 사용
    private String negotiable; // 'Y' 또는 'N'
    private Integer categoryId;
    private String itemCondition;
    private String description;
    private String tradeType;
    private Integer regionId; // HTML에서 지역 이름을 ID로 변환해야 함
    private String safePayYn; // 'Y' 또는 'N'
}
package amgn.amu.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListingsDto {
	
	private int listingId      ;
	private int sellerId       ;
	private Integer categoryId     ;
	private String title           ;
	private String description     ;
	private BigDecimal price           ;
	private String currency        ;
	private String itemCondition  ;
	private String negotiable      ;
	private String tradeType      ;
	private String status          ;
	private Integer regionId       ;
	private String addressText    ;
	private Integer lat             ;
	private Integer lng             ;
	private String safePayYn     ;
	private int viewCount      ;
	private int wishCount      ;
	private int createdAt      ;
	private int updatedAt      ;
	
}

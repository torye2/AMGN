package amgn.amu.dto;

import amgn.amu.entity.Listing;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Long listingId;
    private Long sellerId;
    private List<PhotoDto> photos;
    private List<String> photoUrls;
    private String sellerNickname;
    private String status;
    private String regionName;

    @Getter
    @Setter
    public static class PhotoDto {
        private Long photoId;
        private String url;
    }

    public Listing toEntity() {
        return Listing.builder()
                .listingId(this.listingId)
                .title(this.title)
                .price(this.price)
                .negotiable(this.negotiable)
                .categoryId(this.categoryId)
                .itemCondition(this.itemCondition)
                .description(this.description)
                .tradeType(this.tradeType)
                .regionId(this.regionId)
                .safePayYn(this.safePayYn)
                .sellerId(this.sellerId)
                .status(this.status)
                .build();
    }

    /** 엔티티 -> DTO 기본 매핑 */
    public static ListingDto from(Listing e) {
        if (e == null) return null;

        return ListingDto.builder()
                .listingId(e.getListingId())
                .title(e.getTitle())
                .price(e.getPrice())
                .negotiable(e.getNegotiable())
                .categoryId(e.getCategoryId())
                .itemCondition(e.getItemCondition())
                .description(e.getDescription())
                .tradeType(e.getTradeType())
                .regionId(e.getRegionId())
                .safePayYn(e.getSafePayYn())
                .sellerId(e.getSellerId())
                .status(e.getStatus())
                // .sellerNickname(e.getSeller()!=null ? e.getSeller().getNickname() : null) // 연관관계 있으면
                // .photoUrls(Collections.emptyList()) // 서비스에서 채워도 OK
                .build();
    }
}
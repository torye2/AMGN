package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "listings")
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Long listingId;

    private Long sellerId;
    private Integer categoryId;
    private String title;
    private String description;
    private BigDecimal price;
    private String currency;
    private String itemCondition;
    private String negotiable;
    private String tradeType;
    private String status;
    private Integer regionId;
    private String addressText;
    private String safePayYn;
    private Integer viewCount;
    private Integer wishCount;

    @Column(insertable = false, updatable = false)
    private Timestamp createdAt;

    @Column(insertable = false, updatable = false)
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL)
    private List<ListingPhoto> photos;

}
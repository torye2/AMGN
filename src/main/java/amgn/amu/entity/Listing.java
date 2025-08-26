package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "listings")
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long listingId;

    private Long sellerId; // user_id와 매핑
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
}
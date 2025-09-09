package amgn.amu.entity;

import amgn.amu.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "seller_id")
    private Long sellerId;
    private Integer categoryId;
    private String title;
    private String description;
    private BigDecimal price;
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "KRW";
    private String itemCondition;
    private String negotiable;
    private String tradeType;
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACTIVE";
    private Integer regionId;
    private String addressText;
    private String safePayYn;
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;
    @Column(name = "wish_count", nullable = false)
    @Builder.Default
    private Integer wishCount = 0;

    @CreationTimestamp
    @Column(insertable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(insertable = false)
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL)
    private List<ListingPhoto> photos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private User seller;

    @PrePersist
    public void prePersist() {
        if (currency == null || currency.isBlank()) {currency = "KRW";}
        if (status == null || status.isBlank()){status = "ACTIVE";}
        if (viewCount == null) {viewCount = 0;}
        if (wishCount == null) {wishCount = 0;}
    }
}
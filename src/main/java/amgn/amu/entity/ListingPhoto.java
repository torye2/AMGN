package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "listing_photos")
public class ListingPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long photoId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", referencedColumnName = "listingId")
    private Listing listing;

    private String url;
    private Integer sortOrder;
    private Timestamp createdAt;
    
    public Long getListingId() {
        return listing != null ? listing.getListingId() : null;
    }
}
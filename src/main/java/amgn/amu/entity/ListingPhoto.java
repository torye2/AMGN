package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

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
    @JoinColumn(name = "listing_id", referencedColumnName = "listing_id", nullable = false)
    private Listing listing;

    private String url;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    @CreationTimestamp
    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    @PrePersist
    public void prePersist() {
        if (sortOrder == null) {sortOrder = 0;}
    }
}
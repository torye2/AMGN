package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Getter @Setter
@Entity
@Table(name = "listing_wishes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"listing_id","user_id"}))
public class ListingWish {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wishId;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(insertable = false, updatable = false)
    private Timestamp createdAt;
}

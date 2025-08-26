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

    private Long listingId; // listings 테이블의 listing_id와 매핑
    private String url;
    private Integer sortOrder;
    private Timestamp createdAt;
}
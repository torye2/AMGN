package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "listing_attrs")
public class ListingAttr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attrId;

    private Long listingId; // listings 테이블의 listing_id와 매핑
    private String attrKey;
    private String attrValue;
}
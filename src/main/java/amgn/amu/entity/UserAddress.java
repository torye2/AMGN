package amgn.amu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_addresses")
@Getter @Setter
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "address_line1", nullable = false, length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType = AddressType.OTHER;

    @Column(name = "recipient_name", length = 50)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "detail_address", length = 255, insertable = false, updatable = false)
    private String detailAddress;

    @Column(name="addr_key", insertable=false, updatable=false)
    private String addrKey;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AddressStatus status = AddressStatus.ACTIVE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // MySQL 생성(계산) 컬럼: is_default=1이면 1, 아니면 NULL
    @Column(name = "default_rank", insertable = false, updatable = false)
    private Byte defaultRank;

    public enum AddressType { HOME, WORK, SHIPPING, BILLING, OTHER }
    public enum AddressStatus { ACTIVE, DELETED }
}

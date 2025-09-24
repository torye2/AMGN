package amgn.amu.entity;

import java.time.LocalDateTime;
import java.util.List;

import amgn.amu.domain.User;
import amgn.amu.dto.OrderDto.OrderStatus;
import amgn.amu.dto.OrderDto.TradeMethod;
import amgn.amu.dto.PaymentRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "listing_id")
    private Long listingId;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "final_price")
    private Long finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "method")
    private TradeMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method") // 새 필드
    private PaymentRequest.PaymentMethod paymentMethod;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "recv_name")
    private String receiverName;

    @Column(name = "recv_phone")
    private String receiverPhone;

    @Column(name = "recv_addr1")
    private String receiverAddress1;

    @Column(name = "recv_addr2")
    private String receiverAddress2;

    @Column(name = "recv_zip")
    private String receiverZip;

    @Column(name = "imp_uid")
    private String impUid; //

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    private Listing listing;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<Review> reviews;

    public boolean isReviewed() {
        return reviews != null && !reviews.isEmpty();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private User buyer;
}

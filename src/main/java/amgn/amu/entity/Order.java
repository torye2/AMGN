package amgn.amu.entity;

import java.time.LocalDateTime;

import amgn.amu.dto.OrderDto.OrderStatus;
import amgn.amu.dto.OrderDto.TradeMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private TradeMethod method;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

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
    
//    @Column(name = "meetup_time")
//    private LocalDateTime meetupTime;

//    @Column(name = "meetup_place")
//    private String meetupPlace;

}

package amgn.amu.entity;

import java.time.LocalDateTime;

import amgn.amu.dto.OrderDto.OrderStatus;
import amgn.amu.dto.OrderDto.TradeMethod;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
    private Long id;

    private Long listingId;
    private Long buyerId;
    private Long sellerId;
    private Long finalPrice;

    @Enumerated(EnumType.STRING)
    private TradeMethod method;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Long version; // 낙관적 락 적용
}


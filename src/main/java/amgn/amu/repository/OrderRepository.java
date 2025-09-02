package amgn.amu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import amgn.amu.dto.OrderDto.OrderStatus;
import amgn.amu.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId);

    boolean existsByListingIdAndStatusIn(Long listingId, List<OrderStatus> statuses);

    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    
    List<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}

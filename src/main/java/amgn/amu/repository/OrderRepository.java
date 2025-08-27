package amgn.amu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import amgn.amu.dto.OrderDto.OrderStatus;
import amgn.amu.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
	// @Lock(LockModeType.OPTIMISTIC) Optional<Order> findById(Long id);
	  // 구매자 혹은 판매자의 주문 리스트 조회 (기존 메서드)
	  List<Order> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId);
	  
	    // 특정 listing에 대해 거래 중인 주문이 존재하는지 체크
	    boolean existsByListingIdAndStatusIn(Long listingId, List<OrderStatus> statuses);

	    
	}
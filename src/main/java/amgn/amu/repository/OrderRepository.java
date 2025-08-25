package amgn.amu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import amgn.amu.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
	  @Lock(LockModeType.OPTIMISTIC) Optional<Order> findById(Long id);
	  List<Order> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId);
	}
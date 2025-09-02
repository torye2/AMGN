package amgn.amu.repository;

import amgn.amu.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 특정 구매자와 판매자가 특정 상품에 대해 만든 방 조회
    Optional<ChatRoom> findByListingIdAndBuyerIdAndSellerId(Long listingId, Long buyerId, Long sellerId);

    // 판매자가 관련된 모든 방 조회
    List<ChatRoom> findBySellerId(Long sellerId);

    // 구매자가 관련된 모든 방 조회
    List<ChatRoom> findByBuyerId(Long buyerId);

    // 판매자 + 상품 기준 방 조회 (같은 상품에 여러 구매자가 문의한 경우)
    List<ChatRoom> findByListingIdAndSellerId(Long listingId, Long sellerId);

    // (선택) 구매자 + 상품 기준 방 조회
    List<ChatRoom> findByListingIdAndBuyerId(Long listingId, Long buyerId);
}
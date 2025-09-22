package amgn.amu.service;

import java.util.List;

import amgn.amu.dto.ListingDto;
import amgn.amu.dto.OrderCreateRequest;
import amgn.amu.dto.OrderDto;
import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.TrackingInputRequest;

public interface OrderService {
	  OrderDto create(Long actorUserId, OrderCreateRequest req);                   // -> CREATED
	  OrderDto pay(Long buyerId, Long orderId, PaymentRequest req);                // -> PAID
	  OrderDto confirmMeetup(Long actorUserId, Long orderId);                      // -> MEETUP_CONFIRMED (직거래)
	  OrderDto inputTracking(Long sellerId, Long orderId, TrackingInputRequest r); // (배송형) 운송장 입력 & shippedAt 세팅
	  OrderDto confirmDelivered(Long buyerId, Long orderId);                       // (배송형) -> DELIVERED
	  OrderDto complete(Long actorUserId, Long orderId);                           // -> COMPLETED (정산/리뷰 가능)
	  OrderDto cancel(Long actorUserId, Long orderId);                             // 규칙에 따라 CANCELLED
	  OrderDto dispute(Long actorUserId, Long orderId, String reason);             // -> DISPUTED
	  List<OrderDto> myOrders(Long userId);
	  ListingDto getListingInfo(Long listingId);
	  boolean isListingInTransaction(Long listingId);
	  void deleteOrder(Long userId, Long orderId);
	  List<OrderDto> getSellOrders(Long userId);									// 판매 내역 조회
	  List<OrderDto> getBuyOrders(Long userId);										// 구매 내역 조회
	  OrderDto revertCancel(Long userId, Long orderId);
	  OrderDto getOrder(Long userId, Long orderId);                               // 주문을 ID로 조회
	  
	}

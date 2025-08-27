package amgn.amu.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import amgn.amu.dto.OrderDto.TradeMethod;

public record OrderCreateRequest(@NotNull Long listingId, Long offerId, @NotNull TradeMethod method, String recvName,
		String recvPhone, String recvAddr1, String recvAddr2, String recvZip
		// , LocalDateTime meetupTime, String meetupPlace
		) {


}
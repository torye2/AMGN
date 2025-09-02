package amgn.amu.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatRoomDto {
    private Long roomId;
    private Long listingId;
    private Long buyerId;
    private Long sellerId;
    private String status;
    private LocalDateTime createdAt;
}

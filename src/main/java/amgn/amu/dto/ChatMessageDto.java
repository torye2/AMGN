package amgn.amu.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private Long roomId;
    private Long listingId;
    private Long senderId; // 로그인 사용자 ID
    private Long buyerId;
    private Long sellerId;
    private String senderNickName;
    private String msgType = "TEXT";
    private String content;
    private LocalDateTime createdAt;
}

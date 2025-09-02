package amgn.amu.controller;

import amgn.amu.dto.ChatMessageDto;
import amgn.amu.dto.ChatRoomDto;
import amgn.amu.dto.LoginUserDto;
import amgn.amu.entity.ChatRoom;
import amgn.amu.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 실시간 메시지 전송
    @MessageMapping("/chat/send")
    @SendTo("/topic/messages")
    public ChatMessageDto sendMessage(ChatMessageDto dto, StompHeaderAccessor accessor) {
        Object loginUserObj = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get("loginUser")
                : null;

        if (loginUserObj == null) {
            throw new IllegalStateException("로그인하지 않은 사용자");
        }

        LoginUserDto loginUser = (LoginUserDto) loginUserObj;
        Long buyerId = loginUser.getUserId();

        // 메시지 저장 (채팅방이 없으면 생성)
        ChatMessageDto savedDto = chatService.saveMessage(dto, dto.getListingId(), buyerId, dto.getSellerId());
        return savedDto;
    }

    // 이전 메시지 불러오기
    @GetMapping("/chat/messages")
    public ResponseEntity<List<ChatMessageDto>> getPreviousMessages(
            @RequestParam Long listingId,
            @RequestParam Long sellerId,
            HttpSession session
    ) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }

        Long buyerId = loginUser.getUserId();
        ChatRoom room = chatService.getOrCreateRoom(listingId, buyerId, sellerId);

        if (room == null) {
            return ResponseEntity.ok(List.of());
        }

        List<ChatMessageDto> messages = chatService.getMessagesByRoomId(room.getRoomId());
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/chat/rooms")
    public ResponseEntity<List<ChatRoomDto>> getSellerChatRooms(HttpSession session) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) return ResponseEntity.status(401).build();

        Long sellerId = loginUser.getUserId();
        List<ChatRoomDto> rooms = chatService.getRoomsBySellerId(sellerId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/api/chat/rooms")
    public ResponseEntity<List<ChatRoom>> getChatRooms(@RequestParam Long userId) {
        // 판매자 또는 구매자가 속한 방 모두 조회
        List<ChatRoom> rooms = chatService.getRoomsByUserId(userId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/api/chat/room")
    public ResponseEntity<ChatRoom> getRoom(@RequestParam Long roomId) {
        ChatRoom room = chatService.getRoomById(roomId);
        if(room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(room);
    }
}
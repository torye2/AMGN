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

    // 실시간 메시지 전송: 프론트에서 roomId, content만 보내면 됨
    @MessageMapping("/chat/send")
    @SendTo("/topic/messages")
    public ChatMessageDto sendMessage(ChatMessageDto dto, StompHeaderAccessor accessor) {
        Object loginUserObj = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get("loginUser")
                : null;

        if (loginUserObj == null) {
            throw new IllegalStateException("로그인하지 않은 사용자");
        }

        if (dto.getRoomId() == null) {
            throw new IllegalStateException("roomId가 필요합니다.");
        }

        LoginUserDto loginUser = (LoginUserDto) loginUserObj;
        Long senderId = loginUser.getUserId(); // 보낸 사람은 항상 로그인 유저

        // roomId 기준으로 해당 방에만 메시지 저장 (방 생성 X)
        return chatService.saveMessageToRoom(dto.getRoomId(), senderId, dto);
    }

    // 이전 메시지 불러오기: roomId로만 조회 (방 생성 X)
    @GetMapping("/chat/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @RequestParam Long roomId,
            HttpSession session
    ) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) return ResponseEntity.status(401).build();

        List<ChatMessageDto> messages = chatService.getMessagesByRoomId(roomId);
        return ResponseEntity.ok(messages);
    }

    // (선택) 판매자 채팅 목록 - 세션에서 판매자 id 사용
    @GetMapping("/chat/rooms")
    public ResponseEntity<List<ChatRoomDto>> getSellerRooms(HttpSession session) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) return ResponseEntity.status(401).build();

        Long sellerId = loginUser.getUserId();
        List<ChatRoomDto> rooms = chatService.getRoomsBySellerId(sellerId);
        return ResponseEntity.ok(rooms);
    }

    // 판매자/구매자 공용: userId로 내가 속한 모든 방
    @GetMapping("/api/chat/rooms")
    public ResponseEntity<List<ChatRoom>> getChatRooms(@RequestParam Long userId) {
        List<ChatRoom> rooms = chatService.getRoomsByUserId(userId);
        return ResponseEntity.ok(rooms);
    }

    // roomId로 방 단건 조회
    @GetMapping("/api/chat/room")
    public ResponseEntity<ChatRoom> getRoom(@RequestParam Long roomId) {
        ChatRoom room = chatService.getRoomById(roomId);
        if (room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(room);
    }

    @PostMapping("/api/chat/room/open")
    public ResponseEntity<ChatRoom> openRoom(
            @RequestParam Long listingId,
            @RequestParam Long sellerId,
            HttpSession session
    ) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) return ResponseEntity.status(401).build();

        Long buyerId = loginUser.getUserId();
        ChatRoom room = chatService.getOrCreateRoom(listingId, buyerId, sellerId);
        return ResponseEntity.ok(room);
    }
}

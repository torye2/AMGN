package amgn.amu.service;

import amgn.amu.domain.User;
import amgn.amu.dto.ChatMessageDto;
import amgn.amu.dto.ChatRoomDto;
import amgn.amu.entity.ChatMessage;
import amgn.amu.entity.ChatRoom;
import amgn.amu.repository.ChatMessageRepository;
import amgn.amu.repository.ChatRoomRepository;
import amgn.amu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final UserRepository userRepository;

    // -------------------------------
    // 메시지 저장
    // -------------------------------
    @Transactional
    public ChatMessageDto saveMessage(ChatMessageDto dto, Long listingId, Long buyerId, Long sellerId) {
        // listingId + sellerId 기준으로 방 가져오기 (buyerId는 null일 수 있음)
        ChatRoom room = getOrCreateRoom(listingId, buyerId, sellerId);

        System.out.println("메시지 저장 roomId: " + room.getRoomId());
        ChatMessage message = ChatMessage.builder()
                .room(room)
                .senderId(buyerId)
                .msgType(dto.getMsgType())
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage saved = messageRepository.save(message);

        // DTO 업데이트
        dto.setRoomId(room.getRoomId());
        dto.setSenderId(buyerId);
        dto.setCreatedAt(saved.getCreatedAt());

        // senderNickName 세팅
        User user = userRepository.findById(buyerId).orElse(null);
        dto.setSenderNickName(user != null ? user.getNickName() : "알수없음");

        return dto;
    }

    // -------------------------------
    // 방 생성 또는 기존 방 가져오기
    // -------------------------------
    @Transactional
    public ChatRoom getOrCreateRoom(Long listingId, Long buyerId, Long sellerId) {
        // listingId + sellerId 기준으로 기존 방 조회
        List<ChatRoom> rooms = roomRepository.findByListingIdAndSellerId(listingId, sellerId);

        if (!rooms.isEmpty()) {
            ChatRoom existingRoom = rooms.get(0);

            // buyerId가 비어있으면 업데이트
            if (existingRoom.getBuyerId() == null && buyerId != null) {
                existingRoom.setBuyerId(buyerId);
                return roomRepository.save(existingRoom);
            }

            return existingRoom;
        }

        // 새 방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .listingId(listingId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .status("OPEN")
                .createdAt(LocalDateTime.now())
                .build();

        ChatRoom savedRoom = roomRepository.save(newRoom);
        System.out.println("새 채팅방 생성됨: roomId=" + savedRoom.getRoomId());
        return savedRoom;
    }

    // -------------------------------
    // roomId 기준 메시지 조회
    // -------------------------------
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesByRoomId(Long roomId) {
        return messageRepository.findAllByRoomRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(msg -> {
                    ChatMessageDto dto = new ChatMessageDto();
                    dto.setRoomId(msg.getRoom().getRoomId());
                    dto.setListingId(msg.getRoom().getListingId());
                    dto.setSellerId(msg.getRoom().getSellerId());
                    dto.setSenderId(msg.getSenderId());
                    dto.setContent(msg.getContent());
                    dto.setCreatedAt(msg.getCreatedAt());
                    dto.setMsgType(msg.getMsgType());

                    // senderId로 닉네임 세팅
                    User user = userRepository.findById(msg.getSenderId()).orElse(null);
                    dto.setSenderNickName(user != null ? user.getNickName() : "알수없음");

                    return dto;
                }).toList();
    }

    // -------------------------------
    // 판매자 채팅 목록 조회
    // -------------------------------
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getRoomsBySellerId(Long sellerId) {
        return roomRepository.findBySellerId(sellerId).stream()
                .map(room -> {
                    ChatRoomDto dto = new ChatRoomDto();
                    dto.setRoomId(room.getRoomId());
                    dto.setListingId(room.getListingId());
                    dto.setBuyerId(room.getBuyerId());
                    dto.setSellerId(room.getSellerId());
                    dto.setStatus(room.getStatus());
                    dto.setCreatedAt(room.getCreatedAt());
                    return dto;
                }).toList();
    }

    // -------------------------------
    // 판매자 또는 구매자가 속한 방 모두 조회
    // -------------------------------
    @Transactional(readOnly = true)
    public List<ChatRoom> getRoomsByUserId(Long userId) {
        List<ChatRoom> sellerRooms = roomRepository.findBySellerId(userId);
        List<ChatRoom> buyerRooms = roomRepository.findByBuyerId(userId);

        List<ChatRoom> allRooms = new ArrayList<>();
        allRooms.addAll(sellerRooms);
        allRooms.addAll(buyerRooms);

        return allRooms;
    }

    // -------------------------------
    // roomId 기준 방 조회
    // -------------------------------
    @Transactional(readOnly = true)
    public ChatRoom getRoomById(Long roomId) {
        return roomRepository.findById(roomId).orElse(null);
    }
}
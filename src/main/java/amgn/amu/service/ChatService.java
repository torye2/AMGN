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

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final UserRepository userRepository;

    // -------------------------------
    // roomId로 메시지 저장 (방 생성하지 않음)
    // -------------------------------
    @Transactional
    public ChatMessageDto saveMessageToRoom(Long roomId, Long senderId, ChatMessageDto dto) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalStateException("해당 채팅방이 존재하지 않습니다."));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .senderId(senderId)
                .msgType(dto.getMsgType())
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage saved = messageRepository.save(message);

        ChatMessageDto out = new ChatMessageDto();
        out.setRoomId(room.getRoomId());
        out.setListingId(room.getListingId());
        out.setSellerId(room.getSellerId());
        out.setBuyerId(room.getBuyerId());
        out.setSenderId(senderId);
        out.setContent(saved.getContent());
        out.setMsgType(saved.getMsgType());
        out.setCreatedAt(saved.getCreatedAt());

        userRepository.findById(senderId)
                .ifPresent(u -> out.setSenderNickName(u.getNickName()));

        return out;
    }

    // -------------------------------
    // (초기 대화 시작용) 방 생성 또는 기존 방 가져오기
    //  - 새로 “대화 시작” 버튼 같은 곳에서만 사용하세요.
    // -------------------------------
    @Transactional
    public ChatRoom getOrCreateRoom(Long listingId, Long buyerId, Long sellerId) {
        return roomRepository.findByListingIdAndBuyerIdAndSellerId(listingId, buyerId, sellerId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .listingId(listingId)
                            .buyerId(buyerId)
                            .sellerId(sellerId)
                            .status("OPEN")
                            .createdAt(LocalDateTime.now())
                            .build();
                    return roomRepository.save(newRoom);
                });
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
                    dto.setBuyerId(msg.getRoom().getBuyerId());
                    dto.setSenderId(msg.getSenderId());
                    dto.setContent(msg.getContent());
                    dto.setCreatedAt(msg.getCreatedAt());
                    dto.setMsgType(msg.getMsgType());

                    userRepository.findById(msg.getSenderId())
                            .ifPresent(u -> dto.setSenderNickName(u.getNickName()));

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
        List<ChatRoom> buyerRooms  = roomRepository.findByBuyerId(userId);

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

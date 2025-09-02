package amgn.amu.service;

import amgn.amu.dto.ChatMessageDto;
import amgn.amu.dto.ChatRoomDto;
import amgn.amu.entity.ChatMessage;
import amgn.amu.entity.ChatRoom;
import amgn.amu.repository.ChatMessageRepository;
import amgn.amu.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;

    @Transactional
    public ChatMessageDto saveMessage(ChatMessageDto dto, Long listingId, Long buyerId, Long sellerId) {

        ChatRoom room = getOrCreateRoom(listingId, buyerId, sellerId);
        System.out.println("메시지 저장 roomId: " + room.getRoomId());
        System.out.println(dto + "/" + listingId + "/" + buyerId + "/" + sellerId);
        ChatMessage message = ChatMessage.builder()
                .room(room)
                .senderId(buyerId)
                .msgType(dto.getMsgType())
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage saved = messageRepository.save(message);

        dto.setRoomId(room.getRoomId());
        dto.setSenderId(buyerId);
        dto.setCreatedAt(saved.getCreatedAt());

        return dto;
    }

    /*
    @Transactional
    public ChatRoom getOrCreateRoom(Long listingId, Long buyerId, Long sellerId) {
        //roomRepository.findByListingIdAndBuyerIdAndSellerId(listingId, buyerId, sellerId);
        System.out.println("---------------------------------");
        Optional<ChatRoom> optionalRoom = roomRepository.findByListingIdAndBuyerIdAndSellerId(listingId, buyerId, sellerId);
        if(optionalRoom.isPresent()) {
            System.out.println("채팅방: " + optionalRoom.get());
            return optionalRoom.get();
        } else {
            System.out.println("채팅방 x");
            ChatRoom room = ChatRoom.builder()
                        .listingId(listingId)
                        .buyerId(buyerId)
                        .sellerId(sellerId)
                        .status("OPEN")
                        .createdAt(LocalDateTime.now())
                        .build();
                return roomRepository.save(room);
        }
    }

     */
    @Transactional
    public ChatRoom getOrCreateRoom(Long listingId, Long buyerId, Long sellerId) {
        Optional<ChatRoom> optionalRoom = roomRepository.findByListingIdAndBuyerIdAndSellerId(listingId, buyerId, sellerId);

        if(optionalRoom.isPresent()) {
            System.out.println("기존 채팅방 존재: " + optionalRoom.get().getRoomId());
            return optionalRoom.get();
        } else {
            System.out.println("채팅방 없음: 새로 생성 중... listingId=" + listingId + ", buyerId=" + buyerId + ", sellerId=" + sellerId);
            ChatRoom room = ChatRoom.builder()
                    .listingId(listingId)
                    .buyerId(buyerId)
                    .sellerId(sellerId)
                    .status("OPEN")
                    .createdAt(LocalDateTime.now())
                    .build();
            ChatRoom saved = roomRepository.save(room);
            System.out.println("새 채팅방 생성됨: roomId=" + saved.getRoomId());
            return saved;
        }
    }


    // 메시지 받아오기
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesByRoomId(Long roomId) {
        List<ChatMessageDto> messages = messageRepository.findAllByRoomRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(msg -> {
                    ChatMessageDto dto = new ChatMessageDto();
                    dto.setRoomId(msg.getRoom().getRoomId());
                    dto.setListingId(msg.getRoom().getListingId());
                    dto.setSellerId(msg.getRoom().getSellerId());
                    dto.setSenderId(msg.getSenderId());
                    dto.setContent(msg.getContent());
                    dto.setCreatedAt(msg.getCreatedAt());
                    dto.setMsgType(msg.getMsgType());

                    System.out.println(dto);
                    return dto;
                }).toList();
        System.out.println("총 메시지 개수: " + messages.size());
        return messages;
    }

    // 판매자 채팅 목록
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getRoomsBySellerId(Long sellerId) {
        return roomRepository.findAll().stream()
                .filter(room -> room.getSellerId().equals(sellerId))
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



    @Transactional(readOnly = true)
    public List<ChatRoom> getRoomsByUserId(Long userId) {
        // 판매자 또는 구매자일 경우 모두 조회
        return roomRepository.findByBuyerIdOrSellerId(userId, userId);
    }

    @Transactional(readOnly = true)
    public ChatRoom getRoomById(Long roomId) {
        return roomRepository.findById(roomId).orElse(null);
    }
}
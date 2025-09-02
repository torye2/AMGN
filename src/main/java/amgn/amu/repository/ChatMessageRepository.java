package amgn.amu.repository;

import amgn.amu.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 roomId의 모든 메시지 시간순 조회
    List<ChatMessage> findAllByRoomRoomIdOrderByCreatedAtAsc(Long roomId);
}

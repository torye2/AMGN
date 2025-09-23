package amgn.amu.common;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class DataRetentionPurger {
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void purgeInquiries() { drain("""
            DELETE FROM INQUIRIES
            WHERE PURGE_AT IS NOT NULL AND PURGE_AT < NOW()
            ORDER BY INQUIRY_ID
            LIMIT 1000
            """);
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void purgeChatMessages() { drain("""
        DELETE FROM CHAT_MESSAGES
         WHERE PURGE_AT IS NOT NULL AND PURGE_AT < NOW()
         ORDER BY MESSAGE_ID
         LIMIT 1000
    """);
    }

    @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Seoul")
    public void purgeChatRooms() { drain("""
        DELETE FROM CHAT_ROOMS
         WHERE PURGE_AT IS NOT NULL AND PURGE_AT < NOW()
         ORDER BY ROOM_ID
         LIMIT 1000
    """);
    }

    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Seoul")
    public void purgeReviews() { drain("""
        DELETE FROM REVIEWS
         WHERE PURGE_AT IS NOT NULL AND PURGE_AT < NOW()
         ORDER BY REVIEW_ID
         LIMIT 1000
    """);
    }

    @Scheduled(cron = "0 40 0 * * *", zone = "Asia/Seoul")
    public void purgeInquiryReplies() { drain("""
            DELETE FROM INQUIRY_REPLIES
            WHERE PURGE_AT IS NOT NULL AND PURGE_AT < NOW()
            ORDER BY REPLY_ID
            LIMIT 1000
            """);
    }

    @Scheduled(cron = "0 50 0 * * *", zone = "Asia/Seoul")
    public void purgeOrders() { drain("""
            DELETE FROM ORDERS
            WHERE PURGE_AT IS NOT NULL AND PURGE_AT < NOW()
            ORDER BY ORDER_ID
            LIMIT 1000
            """);
    }

    private void drain(String sql) {
        int total = 0, n;
        do {
            n = jdbcTemplate.update(sql);
            total += n;
        } while (n == 1000); // 더 남아있으면 반복
    }
}

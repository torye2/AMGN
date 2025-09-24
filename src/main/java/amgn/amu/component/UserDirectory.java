package amgn.amu.component;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDirectory {
    private final JdbcTemplate jdbcTemplate;

    public Long findUserIdByNickNameOrThrow(String nickName) {
        return jdbcTemplate.query("""
                SELECT user_id FROM users WHERE nick_name = ? LIMIT 1
                """, ps -> ps.setString(1, nickName),
                rs -> rs.next() ? rs.getLong(1) : null
        );
    }

    public void setUserStatusBanned(Long userId) {
        jdbcTemplate.update("UPDATE users SET status = 'BANNED' WHERE user_id = ?", userId);
    }

    public void setUserStatusActive(Long userId) {
        jdbcTemplate.update("UPDATE users SET status = 'ACTIVE' WHERE user_id = ?", userId);
    }
}

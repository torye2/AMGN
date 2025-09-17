package amgn.amu.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserFollowRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsFollow(Long followerId, Long followingId) {
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_follows WHERE follower_id = ? AND following_id = ?",
                Long.class,
                followerId, followingId
        );
        return (cnt != null && cnt > 0);
    }

    public int insertFollow(Long followerId, Long followingId) {
        return jdbcTemplate.update(
                "INSERT INTO user_follows (follower_id, following_id, created_at) VALUES (?, ?, NOW())",
                followerId, followingId
        );
    }

    public int deleteFollow(Long followerId, Long followingId) {
        return jdbcTemplate.update(
                "DELETE FROM user_follows WHERE follower_id = ? AND following_id = ?",
                followerId, followingId
        );
    }

    // (옵션) 내가 팔로우한 수
    public long countByFollowerId(Long followerId) {
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) AS follower_count FROM user_follows WHERE follower_id = ?",
                Long.class,
                followerId
        );
        return (cnt == null ? 0L : cnt);
    }

    // 상점 주인의 팔로워 수 (following_id 기준)
    public long countFollowers(Long followingId) {
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_follows WHERE following_id = ?",
                Long.class,
                followingId
        );
        return (cnt == null ? 0L : cnt);
    }

    // 팔로워 ID 목록 (following_id 기준 최신순)
    public List<Long> findFollowerIdsByFollowing(Long followingId) {
        return jdbcTemplate.query(
                "SELECT follower_id FROM user_follows WHERE following_id = ? ORDER BY created_at DESC",
                (rs, i) -> rs.getLong(1),
                followingId
        );
    }

    // 내가 팔로우하는 대상 ID 목록 (follower_id 기준 최신순)
    public List<Long> findFollowingIdsByFollower(Long followerId) {
        return jdbcTemplate.query(
                "SELECT following_id FROM user_follows WHERE follower_id = ? ORDER BY created_at DESC",
                (rs, i) -> rs.getLong(1),
                followerId
        );
    }
}

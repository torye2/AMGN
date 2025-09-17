package amgn.amu.service;

import amgn.amu.repository.UserFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserFollowService {

    private final UserFollowRepository repository;

    @Transactional
    public long follow(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new IllegalArgumentException("식별자가 유효하지 않습니다.");
        }
        if (!followerId.equals(followingId) && !repository.existsFollow(followerId, followingId)) {
            repository.insertFollow(followerId, followingId);
        }
        // 상점 주인(followingId)의 팔로워 수 반환
        return repository.countFollowers(followingId);
    }

    @Transactional
    public long unfollow(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new IllegalArgumentException("식별자가 유효하지 않습니다.");
        }
        if (!followerId.equals(followingId)) {
            repository.deleteFollow(followerId, followingId);
        }
        // 상점 주인(followingId)의 팔로워 수 반환
        return repository.countFollowers(followingId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) return false;
        if (followerId.equals(followingId)) return false;
        return repository.existsFollow(followerId, followingId);
    }

    @Transactional(readOnly = true)
    public long getFollowerCount(Long followingId) {
        if (followingId == null) return 0L;
        return repository.countFollowers(followingId);
    }

    @Transactional(readOnly = true)
    public long getFollowingCount(Long followerId) {
        if (followerId == null) return 0L;
        return repository.countByFollowerId(followerId);
    }

    @Transactional(readOnly = true)
    public List<Long> getFollowers(Long followingId) {
        if (followingId == null) return List.of();
        return repository.findFollowerIdsByFollowing(followingId);
    }

    @Transactional(readOnly = true)
    public List<Long> getFollowingList(Long followerId) {
        if (followerId == null) return List.of();
        return repository.findFollowingIdsByFollower(followerId);
    }
}

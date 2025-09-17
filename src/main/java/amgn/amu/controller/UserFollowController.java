package amgn.amu.controller;

import amgn.amu.dto.LoginUserDto;
import amgn.amu.service.UserFollowService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
public class UserFollowController {

    private final UserFollowService followService;

    @PostMapping("/{sellerId}")
    public ResponseEntity<?> follow(@PathVariable("sellerId") Long sellerId, HttpSession session) {
        LoginUserDto login = (LoginUserDto) session.getAttribute("loginUser");
        if (login == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        long count = followService.follow(login.getUserId(), sellerId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @DeleteMapping("/{sellerId}")
    public ResponseEntity<?> unfollow(@PathVariable("sellerId") Long sellerId, HttpSession session) {
        LoginUserDto login = (LoginUserDto) session.getAttribute("loginUser");
        if (login == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        long count = followService.unfollow(login.getUserId(), sellerId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // 상점 주인의 팔로워 수 (following_id = sellerId)
    @GetMapping("/{sellerId}/count")
    public ResponseEntity<?> count(@PathVariable("sellerId") Long sellerId) {
        long count = followService.getFollowerCount(sellerId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // 해당 사용자가 팔로우하는 수 (follower_id = userId)
    @GetMapping("/{userId}/following/count")
    public ResponseEntity<?> followingCount(@PathVariable("userId") Long userId) {
        long count = followService.getFollowingCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // 해당 사용자가 팔로우하는 대상 ID 목록 (follower_id = userId)
    @GetMapping("/{userId}/following")
    public ResponseEntity<?> following(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(Map.of("ids", followService.getFollowingList(userId)));
    }

        // 팔로워 ID 목록 (following_id = sellerId)
        @GetMapping("/{sellerId}/followers")
        public ResponseEntity<?> followers(@PathVariable("sellerId") Long sellerId) {
            return ResponseEntity.ok(Map.of("ids", followService.getFollowers(sellerId)));
        }

    @GetMapping("/{sellerId}/me")
    public ResponseEntity<?> myFollowStatus(@PathVariable("sellerId") Long sellerId, HttpSession session) {
        LoginUserDto login = (LoginUserDto) session.getAttribute("loginUser");
        if (login == null) return ResponseEntity.ok(Map.of("following", false));
        boolean following = followService.isFollowing(login.getUserId(), sellerId);
        return ResponseEntity.ok(Map.of("following", following));
    }
}
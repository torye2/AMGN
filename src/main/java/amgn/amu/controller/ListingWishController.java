package amgn.amu.controller;

import amgn.amu.dto.LoginUserDto;
import amgn.amu.entity.Listing;
import amgn.amu.service.ListingService;
import amgn.amu.service.ListingWishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ListingWishController {

    private final ListingWishService listingWishService;
    private final ListingService listingService;

    /**
     * 현재 로그인 사용자의 찜 여부 + 전체 찜 수 조회
     * GET /product/{listingId}/wish
     */
    @GetMapping("/{listingId}/wish")
    public ResponseEntity<Map<String, Object>> getWishStatus(
            @PathVariable Long listingId,
            HttpSession session
    ) {
        try {
            LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
            Long userId = (loginUser != null) ? loginUser.getUserId() : null;

            boolean wished = (userId != null) && listingWishService.isWishedByUser(listingId, userId);
            long count = listingWishService.getWishCount(listingId);

            return ResponseEntity.ok(Map.of("wished", wished, "count", count));
        } catch (Exception e) {
            log.error("찜 상태 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "찜 상태 조회 실패"));
        }
    }

    /**
     * 찜 토글(로그인 필요, 본인 상품 금지 정책)
     * POST /product/{listingId}/wish
     */
    @PostMapping("/{listingId}/wish")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggleWish(
            @PathVariable Long listingId,
            HttpSession session
    ) {
        try {
            LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }

            Listing listing = listingService.getListingEntity(listingId);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "상품이 존재하지 않습니다."));
            }

            // 정책: 본인 상품 찜 금지 (허용하려면 이 블록 삭제)
            if (Objects.equals(listing.getSellerId(), loginUser.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "본인 상품은 찜할 수 없습니다."));
            }

            boolean wished = listingWishService.toggle(listingId, loginUser.getUserId());
            long count = listingWishService.getWishCount(listingId);
            return ResponseEntity.ok(Map.of("wished", wished, "count", count));
        } catch (Exception e) {
            log.error("찜 토글 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "찜 토글 실패"));
        }
    }

    /**
     * 찜 해제(명시적) – 필요 시 사용
     * DELETE /product/{listingId}/wish
     */
    @DeleteMapping("/{listingId}/wish")
    @Transactional
    public ResponseEntity<Map<String, Object>> removeWish(
            @PathVariable Long listingId,
            HttpSession session
    ) {
        try {
            LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }
            listingWishService.remove(listingId, loginUser.getUserId());
            long count = listingWishService.getWishCount(listingId);
            return ResponseEntity.ok(Map.of("wished", false, "count", count));
        } catch (Exception e) {
            log.error("찜 해제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "찜 해제 실패"));
        }
    }

    /**
     * (옵션) 내가 찜한 상품 ID 목록
     * GET /product/wish/my
     * 프론트에서 이 ID들로 상품 상세를 배치 조회하는 패턴에 유용
     */
    @GetMapping("/wish/my")
    public ResponseEntity<?> myWishes(HttpSession session) {
        try {
            LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }
            // 필요한 경우 WishService에 전용 메서드 만들어서 ID 목록/간단 DTO 리턴
            List<Long> listingIds = listingWishService.getListingIdsByUser(loginUser.getUserId());
            return ResponseEntity.ok(Map.of("listingIds", listingIds));
        } catch (Exception e) {
            log.error("내 찜 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "내 찜 목록 조회 실패"));
        }
    }

    //마이페이지 찜 개수 확인
    @GetMapping("/wish/my/count")
    public ResponseEntity<Map<String, Object>> myWishCount(HttpSession session) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        long count = listingWishService.getWishCountByUser(loginUser.getUserId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}

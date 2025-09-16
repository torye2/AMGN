package amgn.amu.controller;

import amgn.amu.dto.LoginUserDto;
import amgn.amu.dto.ShopInfoResponse;
import amgn.amu.dto.ShopProfileDto;
import amgn.amu.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/{sellerId}")
    public ResponseEntity<ShopInfoResponse> getShop(@PathVariable("sellerId") Long sellerId) {
        return ResponseEntity.ok(shopService.getShopInfo(sellerId));
    }

    /**
     * 상점 프로필 업서트: sellerId가 로그인 사용자와 같을 때만 허용
     * multipart/form-data: intro(문자열), photo|profile_img(파일, 선택)
     * PUT/POST 모두 허용하여 환경별 멀티파트 파싱 이슈를 회피
     */
    @RequestMapping(path = "/{sellerId}", method = {RequestMethod.PUT, RequestMethod.POST}, consumes = {"multipart/form-data"})
    public ResponseEntity<ShopProfileDto> upsertShopProfile(@PathVariable("sellerId") Long sellerId,
                                                            @RequestParam(value = "intro", required = false) String intro,
                                                            @RequestPart(value = "photo", required = false) MultipartFile photo,
                                                            @RequestPart(value = "profile_img", required = false) MultipartFile profileImg,
                                                            HttpSession session) {
        LoginUserDto login = (LoginUserDto) session.getAttribute("loginUser");
        if (login == null) {
            return ResponseEntity.status(401).build();
        }
        if (!sellerId.equals(login.getUserId())) {
            return ResponseEntity.status(403).build();
        }
        MultipartFile effective = (photo != null && !photo.isEmpty()) ? photo : profileImg;
        ShopProfileDto dto = shopService.upsertUserProfile(sellerId, intro, effective);
        return ResponseEntity.ok(dto);
    }
}

package amgn.amu.controller;

import amgn.amu.dto.ShopInfoResponse;
import amgn.amu.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/{sellerId}")
    public ResponseEntity<ShopInfoResponse> getShop(@PathVariable("sellerId") Long sellerId) {
        return ResponseEntity.ok(shopService.getShopInfo(sellerId));
    }
}

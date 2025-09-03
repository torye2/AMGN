package amgn.amu.controller;

import amgn.amu.dto.ListingDto;
import amgn.amu.service.ListingService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
public class SearchRedirectController {

    // (선택) 그냥 리다이렉트만 할 거면 서비스 주입은 필요 없음
    @GetMapping("/search")
    public ResponseEntity<Void> redirectToProductSearch(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "title", required = false) String title
    ) {
        String keyword = (title != null && !title.isBlank()) ? title : query;
        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        URI target = UriComponentsBuilder.fromPath("/searchPage.html")
                .queryParam("title", "{kw}")
                .build()                                   // 인코딩 가정하지 않음
                .expand(keyword)                           // 변수 치환
                .encode(StandardCharsets.UTF_8)            // 여기서 인코딩
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(target).build(); // 302
    }
}

// GeoController.java
package amgn.amu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;            // ★ 스프링 Value 사용 (lombok.Value 아님!)
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    @Value("${kakao.rest.key:}")     // 설정 없으면 빈 문자열
    private String kakaoRestKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 프론트와 동일한 경로로 매핑
    @GetMapping({"/coord2regioncode", "/reverse", "/coord2reverse"})
    public ResponseEntity<?> reverse(@RequestParam double lat, @RequestParam double lng) {
        if (kakaoRestKey == null || kakaoRestKey.isBlank()) {
            return ResponseEntity.status(500).body(Map.of("error", "KAKAO_KEY_MISSING"));
        }

        String url = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?y="+lat+"&x="+lng;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> kakao = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            List<Map<String, Object>> docs = (List<Map<String, Object>>) kakao.getBody().get("documents");

            Map<String, Object> pick = null;
            if (docs != null) {
                pick = docs.stream().filter(d -> "H".equals(d.get("region_type"))).findFirst().orElse(null);
                if (pick == null) pick = docs.stream().filter(d -> "B".equals(d.get("region_type"))).findFirst().orElse(null);
            }
            if (pick == null) return ResponseEntity.status(404).body(Map.of("error", "not_found"));

            String r1 = (String) pick.get("region_1depth_name");
            String r2 = (String) pick.get("region_2depth_name");
            String r3 = (String) pick.get("region_3depth_name");
            String label = java.util.stream.Stream.of(r1, r2, r3)
                    .filter(Objects::nonNull).reduce((a,b)->a+" > "+b).orElse("");

            return ResponseEntity.ok(Map.of("label", label, "lat", lat, "lng", lng));
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // ★ kakao 에러 그대로 전달 (status/본문 포함)
            return ResponseEntity.status(e.getRawStatusCode())
                    .body(Map.of("error", "kakao_error", "kakao_body", e.getResponseBodyAsString()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "internal_error"));
        }
    }
}

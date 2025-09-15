package amgn.amu.controller;

import amgn.amu.entity.Region;
import amgn.amu.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionResolveController {

    private final RegionRepository regionRepository;

    @GetMapping("/resolve")
    public ResponseEntity<?> resolve(@RequestParam String label) {
        // ex) "경기도 > 성남시 수정구 > 태평2동"
        String norm = normalizeSep(label); // "경기도 성남시 수정구 태평2동"
        String[] parts = norm.split("\\s+");
        String r1 = parts.length > 0 ? parts[0] : "";
        String r2 = parts.length > 1 ? parts[1] + (parts.length > 2 && parts[2].endsWith("구") ? " " + parts[2] : "") : ""; // 대충 합치기
        String r3 = norm.replaceFirst(".*\\s", ""); // 마지막 토큰

        String baseDong = stripDongNumber(r3); // "태평2동" -> "태평동"
        String[] cityGu = splitCityGu(r2);     // "성남시 수정구" -> ["성남시","수정구"]

        List<String> candidates = List.of(
                String.join(" ", compact(r1, cityGu[0], cityGu[1], r3)),
                String.join(" ", compact(r1, cityGu[0], cityGu[1], baseDong)),
                String.join(" ", compact(cityGu[0], cityGu[1], r3)),
                String.join(" ", compact(cityGu[0], cityGu[1], baseDong)),
                r3, baseDong
        );

        Region best = null; int bestScore = -1;
        for (String q : candidates) {
            List<Region> rows = regionRepository.findNormalized(q, PageRequest.of(0, 10));
            if (rows.isEmpty()) rows = regionRepository.findLoose(q, PageRequest.of(0, 10));
            for (Region r : rows) {
                int s = score(r, r1, cityGu[0], cityGu[1], baseDong);
                if (s > bestScore) { bestScore = s; best = r; }
            }
            if (bestScore >= 5) break;
        }
        if (best == null) return ResponseEntity.status(404).body(Map.of("error", "not_found", "label", label));
        return ResponseEntity.ok(Map.of("id", best.getId(), "path", best.getPath(), "name", best.getName()));
    }

    private static String normalizeSep(String s){
        return s.replace(">", " ").replace("/", " ").replaceAll("\\s+", " ").trim();
    }
    private static String stripDongNumber(String s){
        return s == null ? "" : s.replaceAll("(\\d+)(?=동)", ""); // "태평2동" -> "태평동"
    }
    private static String[] splitCityGu(String s){
        if (s == null) return new String[]{"",""};
        var m = s.trim().split("\\s+");
        if (m.length >= 2 && m[m.length-1].endsWith("구")) {
            String gu = m[m.length-1];
            String city = String.join(" ", Arrays.copyOf(m, m.length-1));
            return new String[]{ city, gu };
        }
        return new String[]{ s, "" };
    }
    private static List<String> compact(String... a){
        List<String> out = new ArrayList<>();
        for (String x : a) if (x != null && !x.isBlank()) out.add(x.trim());
        return out;
    }
    private static int score(Region r, String r1, String city, String gu, String dong){
        String t = (Optional.ofNullable(r.getPath()).orElse(r.getName())).toLowerCase();
        int s=0;
        if (!dong.isBlank()  && t.contains(dong.toLowerCase()))  s+=3;
        if (!gu.isBlank()    && t.contains(gu.toLowerCase()))    s+=2;
        if (!city.isBlank()  && t.contains(city.toLowerCase()))  s+=1;
        if (!r1.isBlank()    && t.contains(r1.toLowerCase()))    s+=1;
        if (t.matches(".*(동|면|리)\\s*$")) s+=1;
        return s;
    }
}

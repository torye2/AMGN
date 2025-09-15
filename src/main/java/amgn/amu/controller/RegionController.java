package amgn.amu.controller;

import amgn.amu.dto.RegionDto;
import amgn.amu.entity.Region;
import amgn.amu.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RegionController {
    private final JdbcTemplate jdbc;
    private final RegionRepository regionRepository;

    @GetMapping("/regions")
    public List<Map<String, Object>> regions() {
        return jdbc.queryForList(
                "SELECT region_id AS regionId, name, level_no, parent_id, path FROM regions ORDER BY level_no, name"
        );
    }

    @GetMapping("/suggest")
    public List<RegionDto> suggest(@RequestParam(defaultValue = "") String q,
                                   @RequestParam(defaultValue = "20") int limit,
                                   @RequestParam(defaultValue = "true") boolean onlyLeaf) {
        String kw = "%" + q.trim() + "%";
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), 50));

        List<Region> rows = onlyLeaf
                ? regionRepository.findLeafSuggest(kw, pageable)       // ← 말단만
                : regionRepository.findTopByKeyword(kw, pageable);     // ← 기존 전체(필요 시)

        return rows.stream().map(RegionDto::from).toList();
    }

}

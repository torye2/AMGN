package amgn.amu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RegionController {
    private final JdbcTemplate jdbc;

    @GetMapping("/regions")
    public List<Map<String, Object>> regions() {
        return jdbc.queryForList(
                "SELECT region_id AS regionId, name, level_no, parent_id, path FROM regions ORDER BY level_no, name"
        );
    }
}

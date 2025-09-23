package amgn.amu.controller;

import amgn.amu.repository.TopSellerRow;
import amgn.amu.service.AwardService;
import amgn.amu.dto.SellerOfMonthDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/awards")
public class AwardController {

    private final AwardService awardService;

    @GetMapping("/top-sellers")
    public ResponseEntity<?> topSellers(@RequestParam(defaultValue = "units") String metric,
                                        @RequestParam(defaultValue = "3") int limit) {
        // 현재는 units만 지원. 필요시 metric 분기 추가
        List<TopSellerRow> list = awardService.getTopSellersByUnits(limit);
        if (list.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(list);
    }
}

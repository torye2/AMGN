package amgn.amu.controller;

import amgn.amu.dto.ListingSummaryResponse;
import amgn.amu.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/listings")
public class ListingSearchController {

    private final ListingRepository listingRepository;

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ListingSummaryResponse> searchByTitle(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);

        // 검색어의 모든 공백 제거. null은 그대로 유지
        String normalized = (title == null) ? null : title.replaceAll("\\s+", "");

        // Repository의 @Param("q") 에 매핑될 값으로 전달
        return listingRepository.searchByTitleSummary(normalized, pageable);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by("createdAt").descending();
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        boolean desc = parts.length < 2 || parts[1].trim().equalsIgnoreCase("desc");
        return desc ? Sort.by(field).descending() : Sort.by(field).ascending();
    }
}

package amgn.amu.controller;

import amgn.amu.entity.Listing;
import amgn.amu.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/listings")
public class ListingSearchController {

    private final ListingRepository listingRepository;

    @GetMapping(value = "/search", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.data.domain.Page<Listing> searchByTitle(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        org.springframework.data.domain.Sort sortSpec = parseSort(sort);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sortSpec);
        return listingRepository.searchByTitle(title, pageable);
    }

    private org.springframework.data.domain.Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return org.springframework.data.domain.Sort.by("createdAt").descending();
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        boolean desc = parts.length < 2 || parts[1].trim().equalsIgnoreCase("desc");
        return desc ? org.springframework.data.domain.Sort.by(field).descending() : org.springframework.data.domain.Sort.by(field).ascending();
    }
}
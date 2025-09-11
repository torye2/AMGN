package amgn.amu.service;

import amgn.amu.dto.ListingSummaryResponse;
import amgn.amu.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListingSearchService {

    private final ListingRepository listingRepository;

    public Page<amgn.amu.dto.ListingSummaryResponse> searchByTitle(
            String title,
            int page,
            int size,
            String sort // 예: "createdAt,desc"
    ) {
        Sort sortSpec = Sort.by("createdAt").descending();
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            boolean desc = parts.length < 2 || parts[1].trim().equalsIgnoreCase("desc");
            sortSpec = desc ? Sort.by(field).descending() : Sort.by(field).ascending();
        }

        Pageable pageable = PageRequest.of(page, size, sortSpec);

        return listingRepository
                .searchByTitle((title == null || title.isBlank()) ? null : title, pageable)
                .map(ListingSummaryResponse::from);
    }
}

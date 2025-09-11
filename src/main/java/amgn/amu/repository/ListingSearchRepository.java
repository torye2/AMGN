package amgn.amu.repository;

import amgn.amu.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingSearchRepository {
    Page<Listing> searchByTitle(String title, Pageable pageable);
}

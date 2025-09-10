package amgn.amu.repository;

import amgn.amu.entity.ListingAttr;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingAttrsRepository extends JpaRepository<ListingAttr, Long> {
    void deleteByListing_ListingId(Long listingId);
}
package amgn.amu.repository;

import amgn.amu.entity.Listing;
import amgn.amu.entity.ListingAttr;
import amgn.amu.entity.ListingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {
}

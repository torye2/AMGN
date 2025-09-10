package amgn.amu.repository;

import amgn.amu.entity.ListingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ListingPhotosRepository extends JpaRepository<ListingPhoto, Long> {
    void deleteByListing_ListingIdAndPhotoIdIn(Long listingId, Collection<Long> photoIds);
    void deleteByListing_ListingId(Long listingId);
}
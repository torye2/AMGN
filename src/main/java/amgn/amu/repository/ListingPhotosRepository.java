package amgn.amu.repository;

import amgn.amu.entity.ListingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingPhotosRepository extends JpaRepository<ListingPhoto, Long> {
}
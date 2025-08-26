package amgn.amu.repository;

import java.util.List;



import org.springframework.data.jpa.repository.JpaRepository;

import amgn.amu.entity.Region;



public interface RegionRepository extends JpaRepository<Region, Long> {
	  List<Region> findByParentIdOrderByNameAsc(Long parentId);
	  List<Region> findByLevelNo(Integer levelNo);

}

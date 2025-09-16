package amgn.amu.repository;

import java.util.List;


import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import amgn.amu.entity.Region;
import org.springframework.data.jpa.repository.Query;


public interface RegionRepository extends JpaRepository<Region, Long> {
	  List<Region> findByParentIdOrderByNameAsc(Long parentId);
	  List<Region> findByLevelNo(Integer levelNo);

	// RegionRepository.java
	@Query("""
  		select r from Region r
  		where lower(r.name) like lower(:kw) or lower(r.path) like lower(:kw)
  		order by r.levelNo asc, r.name asc
	""")
	List<Region> findTopByKeyword(@Param("kw") String kw, Pageable pageable);

	@Query("select r.path from Region r where r.id = :id")
	String getPathById(@Param("id") Long id);

	@Query("select r.name from Region r where r.id = :id")
	String getNameById(@Param("id") Long id);

	@Query("select r.id from Region r where r.path like concat(:path, '%')")
	List<Long> findIdsByPathPrefix(@Param("path") String path);

	/** 키워드로 지역 자동완성 (하위가 없는 말단 지역만) */
	@Query("""
        select r from Region r
        where (lower(r.name) like lower(:kw) or lower(r.path) like lower(:kw))
          and not exists (
              select 1 from Region c where c.parentId = r.id
          )
        order by r.path asc, r.name asc
    """)
	List<Region> findLeafSuggest(@Param("kw") String kw, Pageable pageable);

	// RegionRepository
	@Query("""
  	select r from Region r
  	where lower(replace(replace(r.path, '>', ' '), '/', ' '))
        	like lower(concat('%', :kw, '%'))
     	or lower(r.name) = lower(:kw)
  	order by length(r.path) asc
	""")
	List<Region> findNormalized(@Param("kw") String kw, Pageable pageable);

	@Query("""
  	select r from Region r
  	where lower(r.path) like lower(concat('%', :kw, '%'))
     	or lower(r.name) like lower(concat('%', :kw, '%'))
  	order by length(r.path) asc
	""")
	List<Region> findLoose(@Param("kw") String kw, Pageable pageable);

}

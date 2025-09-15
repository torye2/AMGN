package amgn.amu.repository;

import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import amgn.amu.entity.Category;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIdOrderByNameAsc(Long parentId);

    @Query("select c.path from Category c where c.id = :id")
    String getPathById(@Param("id") Long id);

    @Query("select c.name from Category c where c.id = :id")
    String getNameById(@Param("id") Long id);

    @Query("select c.id from Category c where c.path like concat(:path, '%')")
    List<Long> findIdsByPathPrefix(@Param("path") String path);
}

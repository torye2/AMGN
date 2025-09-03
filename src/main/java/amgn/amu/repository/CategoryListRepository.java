package amgn.amu.repository;

import amgn.amu.entity.CategoryList;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryListRepository extends JpaRepository<CategoryList, Long> {
    List<CategoryList> findByParentId(Long parentId);
}

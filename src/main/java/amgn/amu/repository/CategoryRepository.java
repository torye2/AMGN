package amgn.amu.repository;

import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;

import amgn.amu.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIdOrderByNameAsc(Long parentId);
    
}

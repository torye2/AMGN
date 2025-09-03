package amgn.amu.service;

import amgn.amu.entity.CategoryList;
import amgn.amu.repository.CategoryListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryListService {

    private final CategoryListRepository repository;

    public List<CategoryList> getAllCategories() {
        return repository.findAll();
    }

    public List<CategoryList> getSubCategories(Long parentId) {
        return repository.findByParentId(parentId);
    }
}

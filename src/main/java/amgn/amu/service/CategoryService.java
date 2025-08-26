package amgn.amu.service;

import java.util.List;

import org.springframework.stereotype.Service;

import amgn.amu.dto.CategoryDto;
import amgn.amu.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getByParentId(Long parentId) {
        return categoryRepository.findByParentIdOrderByNameAsc(parentId)
                .stream()
                .map(c -> new CategoryDto(c.getId(), c.getName(), c.getParentId(), c.getPath()))
                .toList();
    }
}


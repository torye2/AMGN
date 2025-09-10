package amgn.amu.controller;

import amgn.amu.entity.CategoryList;
import amgn.amu.service.CategoryListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryListService service;

    @GetMapping
    public List<CategoryList> getCategories() {
        return service.getAllCategories();
    }

    /**
     * 특정 부모의 하위 카테고리 목록
     * - 정규식으로 숫자만 허용하여 /api/categories/tree 와 충돌 방지
     */
    @GetMapping("/{parentId:\\d+}")
    public List<CategoryList> getSubCategories(@PathVariable Long parentId) {
        return service.getSubCategories(parentId);
    }
}

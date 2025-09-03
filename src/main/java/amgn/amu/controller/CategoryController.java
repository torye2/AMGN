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

    @GetMapping("/{parentId}")
    public List<CategoryList> getSubCategories(@PathVariable Long parentId) {
        return service.getSubCategories(parentId);
    }
}

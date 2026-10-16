package com.jdr1128.shopping.domain.category.controller;

import com.jdr1128.shopping.domain.category.dto.CategoryRequest;
import com.jdr1128.shopping.domain.category.dto.CategoryResponse;
import com.jdr1128.shopping.domain.category.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> getCategories(@RequestParam(defaultValue = "false") boolean includeDeleted) {
        return categoryService.getAdminCategories(includeDeleted);
    }

    @PatchMapping("/{categoryId}")
    public CategoryResponse updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateAdminCategory(categoryId, request);
    }
}

package com.jdr1128.shopping.domain.category.service;

import com.jdr1128.shopping.domain.category.dto.CategoryRequest;
import com.jdr1128.shopping.domain.category.dto.CategoryResponse;
import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.exception.CategoryNotFoundException;
import com.jdr1128.shopping.domain.category.exception.DuplicateCategoryNameException;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        if(categoryRepository.existsByCategoryName(categoryRequest.getCategoryName())) {
            throw new DuplicateCategoryNameException();
        }

        Category category = new Category(categoryRequest.getCategoryName());
        Category savedCategory = categoryRepository.save(category);

        return CategoryResponse.from(savedCategory);
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findByIsDeletedFalse()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse getCategory(Long categoryId) {
        Category category = findActiveCategory(categoryId);

        return CategoryResponse.from(category);
    }

    //카테고리 수정
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Category category = findActiveCategory(categoryId);

        if (!category.getCategoryName().equals(request.getCategoryName())
            && categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new DuplicateCategoryNameException();
        }

        category.updateCategoryName(request.getCategoryName());


        return CategoryResponse.from(category);
    }

    //카테고리 삭제
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = findActiveCategory(categoryId);

        category.delete();
    }

    private Category findActiveCategory(Long categoryId) {
        return categoryRepository.findByCategoryIdAndIsDeletedFalse(categoryId)
                .orElseThrow(CategoryNotFoundException::new);
    }
}

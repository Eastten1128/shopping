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

    //카테고리 생성
    @Transactional
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {

        //동일한 카테고리명이 존재하면
        if(categoryRepository.existsByCategoryName(categoryRequest.getCategoryName())) {
            //예외처리
            throw new DuplicateCategoryNameException();
        }

        //request 기반으로 카테고리 생성
        Category category = new Category(categoryRequest.getCategoryName());
        //생성한 카테고리를 저장
        Category savedCategory = categoryRepository.save(category);

        //컨트롤러에 response를 전달
        return CategoryResponse.from(savedCategory);
    }

    //카테고리 전체 조회(안지워진것만)
    public List<CategoryResponse> getCategories() {
        //
        return categoryRepository.findByIsDeletedFalse()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    //카테고리 조회
    public List<CategoryResponse> getAdminCategories(boolean includeDeleted) {
        List<Category> categories = includeDeleted
                ? categoryRepository.findAll() : categoryRepository.findByIsDeletedFalse();
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse updateAdminCategory(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);
        if (!category.getCategoryName().equals(request.getCategoryName())
                && categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new DuplicateCategoryNameException();
        }
        // 삭제 상태와 삭제 시각을 유지하면서 이름만 변경한다.
        category.updateCategoryName(request.getCategoryName());
        return CategoryResponse.from(category);
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



    //특정 카테고리 조회(카테고리ID)
    private Category findActiveCategory(Long categoryId) {
        return categoryRepository.findByCategoryIdAndIsDeletedFalse(categoryId)
                .orElseThrow(CategoryNotFoundException::new);
    }
}

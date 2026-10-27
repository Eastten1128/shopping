package com.jdr1128.shopping.domain.category.service;

import com.jdr1128.shopping.domain.category.dto.CategoryRequest;
import com.jdr1128.shopping.domain.category.dto.CategoryResponse;
import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.exception.CategoryNotFoundException;
import com.jdr1128.shopping.domain.category.exception.DuplicateCategoryNameException;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리를 등록할 수 있다.")
    void createCategorySuccess(){
        //given
        CategoryRequest request =  new CategoryRequest();
        ReflectionTestUtils.setField(request, "categoryName", "테스트_상품명");

        Category savedCategory = new Category("테스트_상품명");
        ReflectionTestUtils.setField(savedCategory, "categoryId", 1L);

        when(categoryRepository.existsByCategoryName("테스트_상품명")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        //when
        CategoryResponse response = categoryService.createCategory(request);

        //then
        assertThat(response.getCategoryId()).isEqualTo(1L);
        assertThat(response.getCategoryName()).isEqualTo("테스트_상품명");
    }

    @Test
    @DisplayName("중복 카테코리를 등록할 수 없다.")
    void createCategoryDuplicateName(){
        //given
        CategoryRequest request =  new CategoryRequest();
        ReflectionTestUtils.setField(request, "categoryName", "테스트_상품명");

        when(categoryRepository.existsByCategoryName("테스트_상품명")).thenReturn(true);

        //when & then
        assertThatThrownBy(()-> categoryService.createCategory(request))
                .isInstanceOf(DuplicateCategoryNameException.class);

    }

    @Test
    @DisplayName("존재하지 않는 카테고리 조회 시 예외가 발생한다.")
    void getCategoryFailNotFound(){
        //given
        when(categoryRepository.findByCategoryIdAndIsDeletedFalse(999L))
                .thenReturn(Optional.empty());

        //when & then
        assertThatThrownBy(()-> categoryService.getCategory(999L))
                .isInstanceOf(CategoryNotFoundException.class);


    }

}
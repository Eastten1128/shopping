package com.jdr1128.shopping.domain.category.repository;


import com.jdr1128.shopping.domain.category.entity.Category;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("카테고리를 저장하고 ID로 조회할 수 있다.")
    void saveAndFindCategoryById() {

        //given
        Category category = new Category("키보드");

        Category savedCategory = categoryRepository.save(category);

        //when

        Category foundCategory = categoryRepository.findById(savedCategory.getCategoryId())
                .orElseThrow();

        //then
        assertThat(foundCategory.getCategoryName()).isEqualTo(savedCategory.getCategoryName());
    };

    @Test
    @DisplayName("카테고리명으로 조회할 수 있다.")
    void findByCategoryName(){
        //given
        Category category = new Category("마우스");

        categoryRepository.save(category);

        //when

        Category foundCategory = categoryRepository.findByCategoryName(category.getCategoryName()).orElseThrow();;

        //then
        assertThat(foundCategory.getCategoryName()).isEqualTo("마우스");
    }

    @Test
    @DisplayName("카테고리명의 중복 확인")
    void existsByCategoryName(){
        //given
        Category category = new Category("그래픽카드");

        categoryRepository.save(category);

        //when

        boolean exists = categoryRepository.existsByCategoryName(category.getCategoryName());

        //then
        assertThat(exists).isTrue();
    }
}

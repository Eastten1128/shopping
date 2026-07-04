package com.jdr1128.shopping.domain.category.repository;

import com.jdr1128.shopping.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category,Long> {

    Optional<Category> findByCategoryName(String categoryName);

    boolean existsByCategoryName(String categoryName);

    //
    List<Category> findByIsDeletedFalse();

    Optional<Category> findByCategoryIdAndIsDeletedFalse(Long categoryId);


}

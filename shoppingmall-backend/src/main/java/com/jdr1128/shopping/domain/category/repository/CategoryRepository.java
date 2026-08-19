package com.jdr1128.shopping.domain.category.repository;

import com.jdr1128.shopping.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category,Long> {

    //카테고리 - 카테고리명으로 찾기
    Optional<Category> findByCategoryName(String categoryName);

    //카테고리명이 존재하는지
    boolean existsByCategoryName(String categoryName);

    //삭제되지않은 모든 카테고리 찾기
    List<Category> findByIsDeletedFalse();

    //카테고리 - 카테고리Id & 삭제되지 않은 카테고리 찾기
    Optional<Category> findByCategoryIdAndIsDeletedFalse(Long categoryId);


}

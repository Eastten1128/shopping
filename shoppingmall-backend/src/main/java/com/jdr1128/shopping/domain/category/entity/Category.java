package com.jdr1128.shopping.domain.category.entity;

import com.jdr1128.shopping.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
public class Category extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deleteAt;

    public Category(String categoryName){
        this.categoryName = categoryName;
        this.isDeleted = false;
    }

    //Category-CategoryName 수정
    public void updateCategoryName(String categoryName){
        this.categoryName = categoryName;

    }

    //Category 삭제 - 소프트딜리트
    public void delete(){
        this.isDeleted = true;
        this.deleteAt = LocalDateTime.now();
    }


}

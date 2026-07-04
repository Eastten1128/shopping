package com.jdr1128.shopping.domain.category.entity;

import com.jdr1128.shopping.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
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

    protected Category() {

    }

    public Category(String categoryName){
        this.categoryName = categoryName;
        this.isDeleted = false;
    }

    //Category 수정
    public void updateCategoryName(String categoryName){
        this.categoryName = categoryName;

    }

    //Category 삭제
    public void delete(){
        this.isDeleted = true;
        this.deleteAt = LocalDateTime.now();
    }


}

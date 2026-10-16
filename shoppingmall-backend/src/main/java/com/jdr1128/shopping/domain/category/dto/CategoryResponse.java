package com.jdr1128.shopping.domain.category.dto;

import com.jdr1128.shopping.domain.category.entity.Category;

import java.time.LocalDateTime;

public class CategoryResponse {

    private Long categoryId;
    private String categoryName;
    private boolean deleted;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    public CategoryResponse(Long categoryId,
                            String categoryName,
                            LocalDateTime createAt,
                            LocalDateTime updateAt
    ) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.createAt = createAt;
        this.updateAt = updateAt;
    }

    public static CategoryResponse from(Category category){
        CategoryResponse response = new CategoryResponse(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getCreatedAt(),
                category.getUpdateAt()
        );
        response.deleted = category.isDeleted();
        return response;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }
}

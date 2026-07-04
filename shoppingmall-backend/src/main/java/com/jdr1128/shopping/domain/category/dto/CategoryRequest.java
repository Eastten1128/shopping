package com.jdr1128.shopping.domain.category.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoryRequest {

    @NotBlank(message = "카테고리명은 필수입니다.")
    private String categoryName;

    public CategoryRequest(){

    }

    public String getCategoryName() {
        return categoryName;
    }
}

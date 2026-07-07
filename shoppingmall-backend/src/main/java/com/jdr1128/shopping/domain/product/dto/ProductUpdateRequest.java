package com.jdr1128.shopping.domain.product.dto;

import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 수정Request
 * 카테고리Id (categoryId)
 * 상품명 (ProductName)
 * 가격 (price) 0 이상
 * 재고 (stock) 0 이상
 *
 */
public class ProductUpdateRequest {

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    @NotNull(message = "상품명은 필수입니다.")
    private  String productName;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;

    @NotNull(message = "재고는 필수입니다.")
    @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
    private Integer stock;

    @NotNull(message = "상품 상태는 필수입니다")
    private ProductStatus productStatus;

    public ProductUpdateRequest() {
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getPrice() {
        return price;
    }

    public Integer getStock() {
        return stock;
    }

    public ProductStatus getProductStatus() {
        return productStatus;
    }
}

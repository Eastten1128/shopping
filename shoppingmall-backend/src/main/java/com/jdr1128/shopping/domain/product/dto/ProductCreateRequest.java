package com.jdr1128.shopping.domain.product.dto;


import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

public class ProductCreateRequest {

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    //9단계에서 제거
    //임의로 sellerId 지정 -> 로그인한 사용자를 판매자로 사용.
    /*
    @NotNull(message = "판매자 ID는 필수입니다.")
    private Long sellerId;
    */
    @NotNull(message = "상품 이름은 필수입니다.")
    private String productName;

    @NotNull(message = "상품 가격은 필수입니다.")
    private int price;

    @NotNull(message = "상품 재고수는 필수입니다.")
    private int stock;

    @NotNull(message = "상품 상태는 필수입니다.")
    private ProductStatus productStatus;

    public ProductCreateRequest() {

    }

    public Long getCategoryId() {
        return categoryId;
    }

    /*
    public Long getSellerId() {
        return sellerId;
    }
    */

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

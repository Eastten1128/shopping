package com.jdr1128.shopping.domain.product.dto;

import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;

import java.time.LocalDateTime;

public class ProductResponse {

    private Long productId;
    private Long categoryId;
    private Long sellerId;
    private String productName;
    private String sellerLoginId;
    private int price;
    private int stock;
    private ProductStatus productStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductResponse(Long productId,
                           Long categoryId,
                           Long sellerId,
                           String productName,
                           String sellerLoginId,
                           int price,
                           int stock,
                           ProductStatus productStatus,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt
    ) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.sellerId = sellerId;
        this.productName = productName;
        this.sellerLoginId = sellerLoginId;
        this.price = price;
        this.stock = stock;
        this.productStatus = productStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductResponse from(Product product){
        return new ProductResponse(
                product.getProductId(),
                product.getCategory().getCategoryId(),
                product.getSeller().getMemberId(),
                product.getSeller().getLoginId(),
                product.getProductName(),
                product.getPrice(),
                product.getStock(),
                product.getProductStatus(),
                product.getCreatedAt(),
                product.getUpdateAt()
        );
    }

    public Long getProductId() {
        return productId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSellerLoginId() {
        return sellerLoginId;
    }

    public int getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public ProductStatus getProductStatus() {
        return productStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

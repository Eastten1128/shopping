package com.jdr1128.shopping.domain.product.entity;

import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_status", nullable = false)
    private ProductStatus productStatus;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deleteAt;

    protected Product() {

    }

    public Product(
            Category category,
            Member seller,
            String productName,
            int price,
            int stock,
            ProductStatus productStatus
    ){ this.category = category;
        this.seller = seller;
        this.productName = productName;
        this.price = price;
        this.stock = stock;
        this.productStatus = productStatus;
        this.isDeleted = false;

    }

    public Long getProductId() {
        return productId;
    }

    public Category getCategory() {
        return category;
    }

    public Member getSeller() {
        return seller;
    }

    public String getProductName() {
        return productName;
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

    public boolean isDeleted() {
        return isDeleted;
    }

    public LocalDateTime getDeleteAt() {
        return deleteAt;
    }

    public void updateProduct(
            Category category,
            String productName,
            int price,
            int stock,
            ProductStatus productStatus
    ){
        this.category = category;
        this.productName = productName;
        this.price = price;
        this.stock = stock;
        this.productStatus = productStatus;
    }

    public void delete(){
        this.isDeleted = true;
        this.productStatus = ProductStatus.STOPPED;
        this.deleteAt = LocalDateTime.now();

    }

    //상품 재고 감소
    public void decreaseStock(int quantity){
        this.stock -= quantity;

        if(this.stock == 0){
            this.productStatus = ProductStatus.SOLD_OUT;
        }
    }

}

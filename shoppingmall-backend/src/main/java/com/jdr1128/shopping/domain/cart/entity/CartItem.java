package com.jdr1128.shopping.domain.cart.entity;

import com.jdr1128.shopping.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "cart_items"
)
@EntityListeners(AuditingEntityListener.class)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "cart_price", nullable = false)
    private int cartPrice;

    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected CartItem(){}

    public CartItem(
            Cart cart,
            Product product,
            int quantity,
            int cartPrice
    ){
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        this.cartPrice = cartPrice;
    }

    //변경된 상품 개수 적용
    public void updateQuantity(int quantity){
        this.quantity = quantity;
    }

    //상품 개수 증가
    public void increaseQuantity(int quantity){
        this.quantity = quantity;
    }


}

package com.jdr1128.shopping.domain.cart.dto;

import com.jdr1128.shopping.domain.cart.entity.CartItem;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CartItemResponse {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private int quantity;
    private int cartPrice; //장바구니 상품 당 가걱
    private int currentPrice; //상품 가격
    private int totalPrice; //장바구니 총 가격
    private ProductStatus productStatus;
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;

    public CartItemResponse(
            Long cartItemId,
            Long productId,
            String productName,
            int quantity,
            int cartPrice,
            int currentPrice,
            int totalPrice,
            ProductStatus productStatus,
            LocalDateTime addedAt,
            LocalDateTime updatedAt
    ) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.cartPrice = cartPrice;
        this.currentPrice = currentPrice;
        this.totalPrice = totalPrice;
        this.productStatus = productStatus;
        this.addedAt = addedAt;
        this.updatedAt = updatedAt;
    }

    public static CartItemResponse from(CartItem cartItem) {
        int totalPrice = cartItem.getCartPrice() * cartItem.getQuantity();

        return new CartItemResponse(
                cartItem.getCartItemId(),
                cartItem.getProduct().getProductId(),
                cartItem.getProduct().getProductName(),
                cartItem.getQuantity(),
                cartItem.getCartPrice(),
                cartItem.getProduct().getPrice(),
                totalPrice,
                cartItem.getProduct().getProductStatus(),
                cartItem.getAddedAt(),
                cartItem.getUpdatedAt()
        );
    }
}

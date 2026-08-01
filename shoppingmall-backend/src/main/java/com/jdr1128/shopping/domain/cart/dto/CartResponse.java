package com.jdr1128.shopping.domain.cart.dto;

import com.jdr1128.shopping.domain.cart.entity.Cart;
import com.jdr1128.shopping.domain.cart.entity.CartItem;
import lombok.Getter;

import java.util.List;

@Getter
public class CartResponse {

    private Long cartId;
    private Long memberId;
    private List<CartItemResponse> items;
    private int totalCartPrice;

    public CartResponse(
            Long cartId,
            Long memberId,
            List<CartItemResponse> items,
            int totalCartPrice
    ) {
        this.cartId = cartId;
        this.memberId = memberId;
        this.items = items;
        this.totalCartPrice = totalCartPrice;
    }


    public static CartResponse of(Cart cart, List<CartItem> cartItems){
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(CartItemResponse::from)
                .toList();

        int totalCartPrice = itemResponses.stream()
                .mapToInt(CartItemResponse::getTotalPrice)
                .sum();

        return new CartResponse(
                cart.getCartId(),
                cart.getMember().getMemberId(),
                itemResponses,
                totalCartPrice
        );
    }


}

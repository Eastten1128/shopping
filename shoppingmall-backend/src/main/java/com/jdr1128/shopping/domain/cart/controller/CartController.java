package com.jdr1128.shopping.domain.cart.controller;

import com.jdr1128.shopping.domain.cart.dto.CartItemAddRequest;
import com.jdr1128.shopping.domain.cart.dto.CartItemResponse;
import com.jdr1128.shopping.domain.cart.dto.CartItemUpdateRequest;
import com.jdr1128.shopping.domain.cart.dto.CartResponse;
import com.jdr1128.shopping.domain.cart.service.CartService;
import com.jdr1128.shopping.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    //장바구니 확인
    @GetMapping("/me")
    public ResponseEntity<CartResponse> getMyCart(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CartResponse cartResponse = cartService.getMyCart(userDetails.getMemberId());

        return ResponseEntity.ok(cartResponse);
    }

    //장바구니 품목 추가
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CartItemAddRequest request
    ){
        CartItemResponse response = cartService.addCartItem(userDetails.getMemberId(), request);

        return ResponseEntity
                .created(URI.create("/api/carts/items/" + response.getCartItemId()))
                .body(response);

    }


    //장바구니 수정
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CartItemResponse> updateCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemUpdateRequest request
    ){
        CartItemResponse response = cartService.updateCartItem(
                userDetails.getMemberId(),
                cartItemId,
                request
        );

        return ResponseEntity.ok(response);

    }

    //품목 하나 삭제
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId
    ){
        cartService.deleteCartItem(
                userDetails.getMemberId(),
                cartItemId
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        cartService.clearCart(userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

}

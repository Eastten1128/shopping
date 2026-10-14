package com.jdr1128.shopping.domain.product.controller;

import com.jdr1128.shopping.domain.product.dto.ProductResponse;
import com.jdr1128.shopping.domain.product.dto.ProductUpdateRequest;
import com.jdr1128.shopping.domain.product.service.ProductService;
import com.jdr1128.shopping.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {
    private final ProductService productService;

    public SellerProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getProducts(
            @RequestParam(defaultValue = "false") boolean includeStopped,
            @AuthenticationPrincipal CustomUserDetails user) {
        return productService.getSellerProducts(user.getMemberId(), user.getMember().getRole(), includeStopped);
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId, @AuthenticationPrincipal CustomUserDetails user) {
        return productService.getSellerProduct(productId, user.getMemberId(), user.getMember().getRole());
    }

    @PatchMapping("/{productId}")
    public ProductResponse updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductUpdateRequest request,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        return productService.updateSellerProduct(productId, request, user.getMemberId(), user.getMember().getRole());
    }
}

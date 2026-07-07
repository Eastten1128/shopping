package com.jdr1128.shopping.domain.product.controller;

import com.jdr1128.shopping.domain.product.dto.ProductCreateRequest;
import com.jdr1128.shopping.domain.product.dto.ProductResponse;
import com.jdr1128.shopping.domain.product.dto.ProductUpdateRequest;
import com.jdr1128.shopping.domain.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request){
        ProductResponse response = productService.createProduct(request);

        return ResponseEntity
                .created(URI.create("/api/products/" + response.getProductId()))
                .body(response);
    }

    //모든 상품 검색
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(){
        List<ProductResponse> responses = productService.getProducts();

        return ResponseEntity.ok(responses);
    }

    //특정 상품 검색(ProductId)
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long productId
    ){
        ProductResponse response = productService.getProduct(productId);

        return ResponseEntity.ok(response);
    }

    //상품 수정(ProductId)
    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request
    ){
        ProductResponse response = productService.updateProduct(productId, request);

        return ResponseEntity.ok(response);
    }

    //상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId
    ){
        productService.deleteProduct(productId);

        return ResponseEntity.noContent().build();
    }
}

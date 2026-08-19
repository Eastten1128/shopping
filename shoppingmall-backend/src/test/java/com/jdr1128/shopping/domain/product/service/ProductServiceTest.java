package com.jdr1128.shopping.domain.product.service;

import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.service.MemberService;
import com.jdr1128.shopping.domain.product.dto.ProductUpdateRequest;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import com.jdr1128.shopping.domain.product.expection.ProductAccessDeniedException;
import com.jdr1128.shopping.domain.product.repository.ProductRepository;
import org.aspectj.util.Reflection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;



    @Test
    @DisplayName("SELLER는 남의 상품을 수정할 수 없음")
    void updateProduct_fail_sellerNotOwner(){
        //given
        Long productId = 1L; //상품 번호 지정
        Long loginMemberId = 2L; //로그인 id

        //판매자 Member 객 객체 생성
        Member owner = new Member(
                "seller01",
                "password",
                "seller01@test.com",
                Role.SELLER
        );
        ReflectionTestUtils.setField(owner,"memberId", 1L);

        Category category = new Category("키보드");
        ReflectionTestUtils.setField(category, "categoryId", 1L);

        Product product = new Product(
                category,
                owner,
                "테스트_상품명",
                500000,
                10,
                ProductStatus.SELLING
        );
        ReflectionTestUtils.setField(product, "productId", productId);

        ProductUpdateRequest request = new ProductUpdateRequest();
        ReflectionTestUtils.setField(request, "categoryId", 1L);
        ReflectionTestUtils.setField(request, "productName", "수정 상품");
        ReflectionTestUtils.setField(request, "price", 60000);
        ReflectionTestUtils.setField(request, "stock", 5);
        ReflectionTestUtils.setField(request, "productStatus", ProductStatus.SELLING);

        when(productRepository.findByProductIdAndIsDeletedFalse(productId))
                .thenReturn(Optional.of(product));

        //when & then
        assertThatThrownBy(()->
                productService.updateProduct(
                        productId,
                        request,
                        loginMemberId,
                        Role.SELLER
                )
        ).isInstanceOf(ProductAccessDeniedException.class);

    }

    @Test
    @DisplayName("없는 카테고리로 상품 등록 실패")
    void createProduct_fail_categoryNotFound(){

    }

}
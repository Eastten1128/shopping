package com.jdr1128.shopping.domain.cart.service;

import com.jdr1128.shopping.domain.cart.dto.CartItemAddRequest;
import com.jdr1128.shopping.domain.cart.dto.CartItemResponse;
import com.jdr1128.shopping.domain.cart.entity.Cart;
import com.jdr1128.shopping.domain.cart.entity.CartItem;
import com.jdr1128.shopping.domain.cart.repository.CartItemRepository;
import com.jdr1128.shopping.domain.cart.repository.CartRepository;
import com.jdr1128.shopping.domain.category.entity.Category;
import com.jdr1128.shopping.domain.category.repository.CategoryRepository;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import com.jdr1128.shopping.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("같은 상품을 다시 담으면 기존 장바구니 상품 수량이 증가한다.")
    void updateProductFailSellerNotOwner(){
        //given
        Long memberId = 1L;
        Long productId = 1L;
        Long cartItemId = 1L;

        Member member = new Member(
                "user99",
                "password",
                "user99@test.com",
                Role.USER
                );
        ReflectionTestUtils.setField(member, "memberId", 1L);

        Cart cart = new Cart(member);
        ReflectionTestUtils.setField(cart, "cartId", 1L);

        Category category = new Category("테스트_카테고리");

        Member seller = new Member(
                "seller99",
                "password",
                "seller99@test.com",
                Role.SELLER
        );

        Product product = new Product(
                category,
                seller,
                "테스트_상품명",
                30000,
                100,
                ProductStatus.SELLING
        );
        ReflectionTestUtils.setField(product, "productId", productId);

        //장바구니에 상품 2개 담기
        CartItem existingItem = new CartItem(cart, product,2,30000);
        ReflectionTestUtils.setField(existingItem, "cartItemId", cartItemId);

        CartItemAddRequest request = new CartItemAddRequest();
        //장바구니에 상품 1개 추가
        ReflectionTestUtils.setField(request, "productId", productId);
        ReflectionTestUtils.setField(request, "quantity", 1);

        when(cartRepository.findByMember_MemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(productRepository.findByProductIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(cartItemId, productId))
                .thenReturn(Optional.of(existingItem));

        when(cartItemRepository.save(existingItem))
                .thenReturn(existingItem);

        //when
        CartItemResponse response = cartService.addCartItem(memberId, request);

        //then
        assertThat(response.getQuantity()).isEqualTo(3);
        assertThat(response.getCartPrice()).isEqualTo(30000);
        assertThat(response.getTotalPrice()).isEqualTo(90000);


    }
}
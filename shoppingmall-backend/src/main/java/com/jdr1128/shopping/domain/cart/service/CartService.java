package com.jdr1128.shopping.domain.cart.service;

import com.jdr1128.shopping.domain.cart.dto.CartItemAddRequest;
import com.jdr1128.shopping.domain.cart.dto.CartItemResponse;
import com.jdr1128.shopping.domain.cart.dto.CartItemUpdateRequest;
import com.jdr1128.shopping.domain.cart.dto.CartResponse;
import com.jdr1128.shopping.domain.cart.entity.Cart;
import com.jdr1128.shopping.domain.cart.entity.CartItem;
import com.jdr1128.shopping.domain.cart.exception.CartItemNotFoundException;
import com.jdr1128.shopping.domain.cart.exception.ProductNotAvailableException;
import com.jdr1128.shopping.domain.cart.repository.CartItemRepository;
import com.jdr1128.shopping.domain.cart.repository.CartRepository;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.entity.ProductStatus;
import com.jdr1128.shopping.domain.product.expection.ProductNotFoundException;
import com.jdr1128.shopping.domain.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            MemberRepository memberRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
    }

    //장바구니 불러오기
    @Transactional
    public CartResponse getMyCart(Long memberId){
        Cart cart = findOrCreateCart(memberId);
        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());

        return CartResponse.of(cart, cartItems);
    }

    //장바구니 담기
    @Transactional
    public CartItemResponse addCartItem(
            Long memberId,
            CartItemAddRequest request
    ){
        Cart cart = findOrCreateCart(memberId);

        Product product = productRepository.findByProductIdAndIsDeletedFalse(request.getProductId())
                .orElseThrow(ProductNotFoundException::new);

        //담을 수 있나 검사
        validateProductCanBeAdded(product, request.getQuantity());

        //장바구니에 넣을 상품 리스트 생성
        //와 개어렵네 진짜
        CartItem cartItem = cartItemRepository
                .findByCart_CartIdAndProduct_ProductId(
                        cart.getCartId(),
                        product.getProductId()
                )
                //존재하는 상품이면 개수 더하기
                .map(existingItem -> {
                    int newQuantity = existingItem.getQuantity() + request.getQuantity();
                    validateProductCanBeAdded(product, newQuantity);
                    existingItem.updateQuantity(newQuantity);
                    return existingItem;
                })
                //아니면 새로 담기
                .orElseGet(()-> new CartItem(
                        cart,
                        product,
                        request.getQuantity(),
                        product.getPrice()
                ));

        //cartItem 저장
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        //response 객체 생성
        return CartItemResponse.from(savedCartItem);

    }


    //장바구니 상품 수정
    @Transactional
    public CartItemResponse updateCartItem(
            Long memberId,
            Long cartItemId,
            CartItemUpdateRequest request
    ){
        CartItem cartItem = findByCartItem(memberId, cartItemId);

        validateProductCanBeAdded(cartItem.getProduct(), request.getQuantity());

        cartItem.updateQuantity(request.getQuantity());

        return CartItemResponse.from(cartItem);
    }

    //장바구니의 특정 상품을 장바구니에서 제거
    @Transactional
    public void deleteCartItem(
            Long memberId,
            Long cartItemId
    ) {
        CartItem cartItem = findByCartItem(memberId, cartItemId);

        cartItemRepository.delete(cartItem);
    }

    //장바구니 전체 비우기
    @Transactional
    public void clearCart(Long memberId) {
        Cart cart = findOrCreateCart(memberId);

        cartItemRepository.deleteByCart_CartId(cart.getCartId());
    }

    //장바구니가 있으면 그대로 쓰고
    //없으면 새로 만들어서 저장
    private Cart findOrCreateCart(Long memberId){
        return cartRepository.findByMember_MemberId(memberId)
                .orElseGet(() -> {
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(()->new IllegalArgumentException("회원을 찾을 수 없습니다."));

                    Cart cart = new Cart(member);

                    return cartRepository.save(cart);
                });
    }

    //
    private CartItem findByCartItem(Long memberId, Long cartItemId){
        return cartItemRepository
                .findByCartItemIdAndCart_Member_MemberId(cartItemId, memberId)
                .orElseThrow(CartItemNotFoundException::new);
    }

    //상품 추가가 가능한지 검사하는 메소드
    private void validateProductCanBeAdded(Product product, int quantity){
        //ProductStatus가 SELLING이 아니면
        if(product.getProductStatus() != ProductStatus.SELLING){
            throw new ProductNotAvailableException();
        }

        //수량보다 장바구니 값을 많이 넣으면,
        if(product.getStock() < quantity){
            throw new ProductNotAvailableException();
        }
    }
}

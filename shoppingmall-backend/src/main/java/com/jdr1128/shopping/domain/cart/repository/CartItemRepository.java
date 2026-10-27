package com.jdr1128.shopping.domain.cart.repository;

import com.jdr1128.shopping.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem,Long> {

    List<CartItem> findByCart_CartId(Long cartId);

    Optional<CartItem> findByCart_CartIdAndProduct_ProductId(
            Long cartId,
            Long productId
    );

    Optional<CartItem> findByCartItemIdAndCart_Member_MemberId(
            Long cartItemId,
            Long memberId
    );

    void deleteByCart_CartId(Long cartId);
}

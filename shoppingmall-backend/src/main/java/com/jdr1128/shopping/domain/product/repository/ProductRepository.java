package com.jdr1128.shopping.domain.product.repository;

import com.jdr1128.shopping.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {

    List<Product> findByIsDeletedFalse();

    Optional<Product> findByProductIdAndIsDeletedFalse(Long productId);

    List<Product> findByCategory_CategoryIdAndIsDeletedFalse(Long categoryId);

    List<Product> findBySeller_MemberIdAndIsDeletedFalse(Long sellerId);

}

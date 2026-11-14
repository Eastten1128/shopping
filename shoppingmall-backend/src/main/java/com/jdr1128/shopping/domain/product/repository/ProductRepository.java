package com.jdr1128.shopping.domain.product.repository;

import com.jdr1128.shopping.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Product p
            where p.productId = :productId
            and p.isDeleted = false
            """)
    Optional<Product> findActiveProductForUpdate(@Param("productId") Long productId);


    //삭제되지 않은 모든 상품 검색
    List<Product> findByIsDeletedFalse();

    //상품 - where 상품Id = productId & 삭제 = false
    Optional<Product> findByProductIdAndIsDeletedFalse(Long productId);

    //상품 - 카테고리Id = categoryId & 삭제 = false
    List<Product> findByCategory_CategoryIdAndIsDeletedFalse(Long categoryId);

    //상품 = SellerId = sellerId & 삭제 = false
    List<Product> findBySeller_MemberIdAndIsDeletedFalse(Long sellerId);

    List<Product> findBySeller_MemberId(Long sellerId);

    Optional<Product> findByProductIdAndSeller_MemberId(Long productId, Long sellerId);

}

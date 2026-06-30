package com.jdr1128.shopping.domain.product.repository;

import com.jdr1128.shopping.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product,Long> {

    List<Product> findByCategory_CategoryId(Long categoryId);

    List<Product> findBySeller_MemberId(Long sellerId);
}

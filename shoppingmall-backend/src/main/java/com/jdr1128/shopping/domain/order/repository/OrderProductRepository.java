package com.jdr1128.shopping.domain.order.repository;

import com.jdr1128.shopping.domain.order.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {

    List<OrderProduct> findByOrder_OrderId(Long orderId);
}

package com.jdr1128.shopping.domain.order.repository;

import com.jdr1128.shopping.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByMember_MemberIdOrderByOrderDateDesc(Long memberId);

    Optional<Order> findByOrderIdAndMember_MemberId(Long orderId, Long memberId);
}

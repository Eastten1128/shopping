package com.jdr1128.shopping.domain.payment.repository;

import com.jdr1128.shopping.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrder_OrderId(Long paymentId);

    Optional<Payment> findByPayIdAndOrder_Member_MemberId(
            Long paymentId,
            Long memberId
    );

    Optional<Payment> findByOrder_OrderIdAndOrder_Member_MemberId(
            Long orderId,
            Long memberId
    );
}

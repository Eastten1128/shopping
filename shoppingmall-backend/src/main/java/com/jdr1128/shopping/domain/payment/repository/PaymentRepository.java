package com.jdr1128.shopping.domain.payment.repository;

import com.jdr1128.shopping.domain.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrder_OrderId(Long paymentId);


    //같은 결제(사용자가 같은 주문 정보로 결제 요청을 여러번 보냄 등) 요청이 동시에 상태를 바꾸지 못하도록
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Payment p
            where p.payId = :payId
            and p.order.member.memberId = :memberId
            """)
    Optional<Payment> findMyPaymentForUpdate(
            @Param("memberId") Long memberId,
            @Param("payId") Long payId
    );

    Optional<Payment> findByPayIdAndOrder_Member_MemberId(
            Long paymentId,
            Long memberId
    );

    Optional<Payment> findByOrder_OrderIdAndOrder_Member_MemberId(
            Long orderId,
            Long memberId
    );
}

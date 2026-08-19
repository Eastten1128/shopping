package com.jdr1128.shopping.domain.payment.service;

import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.order.entity.Order;
import com.jdr1128.shopping.domain.order.entity.OrderProduct;
import com.jdr1128.shopping.domain.order.repository.OrderProductRepository;
import com.jdr1128.shopping.domain.order.repository.OrderRepository;
import com.jdr1128.shopping.domain.payment.dto.PaymentRequest;
import com.jdr1128.shopping.domain.payment.dto.PaymentResponse;
import com.jdr1128.shopping.domain.payment.entity.Payment;
import com.jdr1128.shopping.domain.payment.exception.AlreadyPaidException;
import com.jdr1128.shopping.domain.payment.exception.PaymentCancelException;
import com.jdr1128.shopping.domain.payment.exception.PaymentNotFoundException;
import com.jdr1128.shopping.domain.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly=true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderProductRepository orderProductRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderProductRepository = orderProductRepository;
    }

    //주문 결제 요청
    @Transactional
    public PaymentResponse payOrder(
            Long memberId,
            Long orderId,
            PaymentRequest request
    ) {
        Order order = orderRepository.findByOrderIdAndMember_MemberId(orderId, memberId)
                .orElseThrow(PaymentNotFoundException::new);

        if (order.isPaid() || paymentRepository.existsByOrder_OrderId(orderId)) {
            throw new AlreadyPaidException();
        }

        Payment payment = new Payment(
                order,
                request.getCardCompany(),
                request.getCardNumber(),
                order.getTotalOrderPrice()
        );

        if (request.isPaymentSuccess()){
            payment.complete();
            order.pay();
        } else{
            payment.fail();
            order.fail();
            restoreStock(order.getOrderId());
        }

        Payment savedPayment = paymentRepository.save(payment);

        return PaymentResponse.from(savedPayment);
    }

    //결제 정보 조회
    public PaymentResponse getPayment(
            Long memberId,
            Long payId
    ) {
        Payment payment = paymentRepository.findByPayIdAndOrder_Member_MemberId(payId, memberId)
                .orElseThrow(PaymentNotFoundException::new);

        return PaymentResponse.from(payment);
    }

    //결제 취소
    @Transactional
    public PaymentResponse cancelPayment(
            Long memberId,
            Long payId
    )
    {
        Payment payment = paymentRepository.findByPayIdAndOrder_Member_MemberId(
                payId,
                memberId
                )
                .orElseThrow(PaymentNotFoundException::new);

        //
        if (!payment.isComplete()) {
            throw new PaymentCancelException();
        }

        payment.cancel();
        payment.getOrder().cancel();

        restoreStock(payment.getOrder().getOrderId());

        return PaymentResponse.from(payment);
    }

    //재고 원래대로 돌려놓기
    public void restoreStock(Long orderId){
        List<OrderProduct> orderProducts = orderProductRepository.findByOrder_OrderId(orderId);

        for( OrderProduct orderProduct : orderProducts){
            orderProduct.getProduct().increaseStock(orderProduct.getAmount());
        }
    }
}

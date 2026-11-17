package com.jdr1128.shopping.domain.payment.service;

import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.order.entity.Order;
import com.jdr1128.shopping.domain.order.entity.OrderProduct;
import com.jdr1128.shopping.domain.order.entity.OrderStatus;
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

    //Ready 결제 생성
    @Transactional
    public PaymentResponse createPayment(
            Long memberId,
            Long orderId,
            PaymentRequest request
    ) {
        //주문 가져오기
        Order order = orderRepository.findByOrderIdAndMember_MemberId(orderId, memberId)
                .orElseThrow(PaymentNotFoundException::new);

        //주문의 Status == 결제완료 or 이미 그 주문의 결제내역이 존재하면 Exception
        if (order.isPaid() || paymentRepository.existsByOrder_OrderId(orderId)) {
            throw new AlreadyPaidException();
        }

        // 결제는 ORDERED 상태에서만
        if (order.getOrderStatus() != OrderStatus.ORDERED) {
            throw new PaymentCancelException();
        }

        //Payment 생성
        Payment payment = new Payment(
                order,
                request.getCardCompany(),
                request.getCardNumber(),
                order.getTotalOrderPrice()
        );

        //결제 완료시 paymentStatus -> COMPLETE, OrderStatus -> PAY
        //결제 실패시 PaymentStatus -> FAIL, OrderStatus -> FAIL, 재고 복구
        /*
        if (request.isPaymentSuccess()){
            payment.complete();
            order.pay();
        } else{
            payment.fail();
            order.fail();
            restoreStock(order.getOrderId());
        }
         */

        //Payment READY 생성
        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponse.from(savedPayment);
    }

    //Ready 결제 -> Complete 전환
    @Transactional
    public PaymentResponse confirmPayment(Long memberId, Long paymentId) {
        Payment payment = paymentRepository.findMyPaymentForUpdate(memberId, paymentId)
                .orElseThrow(PaymentNotFoundException::new);

        //이미 결제가 완료된 요청의 재시도는 결과 반환
        if(payment.isComplete()){
            return PaymentResponse.from(payment);
        }
        //준비 상태가 아닌 경우 예외 처리
        if (!payment.isReady()){
            throw new PaymentCancelException();
        }

        //재고 임시 차감 - 결제 진행 - 재고 차감 확정
        //Redis 에약 확인 + DB 재고 차감 연결

        //결제 완료
        payment.complete();
        //주문도 PAID 확정
        payment.getOrder().pay();

        return PaymentResponse.from(payment);
    }

    //Ready 결제 -> FAILED 전환
    @Transactional
    public PaymentResponse failPayment(Long memberId, Long paymentId){
        Payment payment = paymentRepository.findMyPaymentForUpdate(memberId, paymentId)
                .orElseThrow(PaymentNotFoundException::new);

        //이미 결제 취소된 경우 response 리턴
        if (payment.isFailed()){
            return PaymentResponse.from(payment);
        }
        //payment -> READY가 아닌 경우 Exception
        if (!payment.isReady()) {
            throw new PaymentCancelException();
        }

        //결제 요청 -> 임시 제고 차감 -> 결제 실패 -> 재고 복구 redis

        //payment 및 해당 Order fail
        payment.fail();
        payment.getOrder().fail();

        return PaymentResponse.from(payment);
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

        //Payment 정보 가져오기
        Payment payment = paymentRepository.findByPayIdAndOrder_Member_MemberId(
                payId,
                memberId
                )
                .orElseThrow(PaymentNotFoundException::new);

        //결제가 완료되지 않았다면 Exception
        if (!payment.isComplete()) {
            throw new PaymentCancelException();
        }

        //결제 취소
        payment.cancel();
        //결제 연동된 주문도 취소
        payment.getOrder().cancel();
        //결제했던 주문의 재고 돌려놓기
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

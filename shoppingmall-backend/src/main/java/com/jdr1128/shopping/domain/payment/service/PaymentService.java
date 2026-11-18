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
import com.jdr1128.shopping.domain.product.entity.Product;
import com.jdr1128.shopping.domain.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final ProductRepository productRepository;
    private final StockReservationService stockReservationService;

    private static final Logger log =
            LoggerFactory.getLogger(PaymentService.class);

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderProductRepository orderProductRepository,
            ProductRepository productRepository,
            StockReservationService stockReservationService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderProductRepository = orderProductRepository;
        this.productRepository = productRepository;
        this.stockReservationService = stockReservationService;
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

        // 결제 행 잠금
        Payment payment = paymentRepository.findMyPaymentForUpdate(memberId, paymentId)
                .orElseThrow(PaymentNotFoundException::new);

        // DB 주문 상품을 합산 정렬
        Map<Long, Integer> quantities = new TreeMap<>();

        List<OrderProduct> orderProducts = orderProductRepository
                .findByOrder_OrderId(payment.getOrder().getOrderId());

        for (OrderProduct item : orderProducts) {
            if (item.getAmount() <= 0 ){
                throw new IllegalStateException("잘못된 주문 수량");
            }

            quantities.merge(
                    item.getProduct().getProductId(),
                    item.getAmount(),
                    Math::addExact
            );
        }

        if (quantities.isEmpty()) {
            throw new IllegalStateException("확정할 주문 상품이 없습니다.");
        }

        String canonicalItems = quantities.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));

        // 3. 이미 DB 확정된 결제는 다시 차감하지 않음
        if (payment.isComplete()) {
            registerConfirmAfterCommit(paymentId, canonicalItems);
            return PaymentResponse.from(payment);
        }
        //준비 상태가 아닌 경우 예외 처리
        if (!payment.isReady()
                || payment.getOrder().getOrderStatus() != OrderStatus.ORDERED) {
            throw new PaymentCancelException();
        }

        // 4. 예약을 확정 처리 중으로 보호
        stockReservationService.beginConfirm(paymentId, canonicalItems);

        // 5. 상품 ID순으로 잠그고 DB 실재고 차감
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productRepository
                    .findActiveProductForUpdate(entry.getKey())
                    .orElseThrow(() ->
                            new IllegalStateException("확정할 상품이 없습니다."));

            product.decreaseStock(entry.getValue());
        }

        //결제 완료
        payment.complete();
        //주문도 PAID 확정
        payment.getOrder().pay();

        // 실제 DB 커밋 이후 Redis 확정 반영
        registerConfirmAfterCommit(paymentId, canonicalItems);


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

    private void registerConfirmAfterCommit(
            Long paymentId,
            String canonicalItems
    ) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            stockReservationService.completeConfirm(
                                    paymentId,
                                    canonicalItems
                            );
                        } catch (Exception e) {
                            log.error(
                                    "DB 결제 완료 후 Redis 확정 재처리 필요: paymentId={}",
                                    paymentId,
                                    e
                            );
                        }
                    }
                }
        );
    }
}

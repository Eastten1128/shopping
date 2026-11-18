package com.jdr1128.shopping.domain.payment.service;


import com.jdr1128.shopping.domain.order.repository.OrderProductRepository;
import com.jdr1128.shopping.domain.payment.dto.PaymentRequest;
import com.jdr1128.shopping.domain.payment.dto.PaymentResponse;
import com.jdr1128.shopping.domain.payment.dto.ReservationItem;
import com.jdr1128.shopping.domain.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

//결제 생성과 재고 예약의 실행 순서 조율
@Service
public class PaymentFacade {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentFacade.class);

    private final PaymentService paymentService;
    private final OrderProductRepository orderProductRepository;
    private final StockReservationService stockReservationService;

    public PaymentFacade(
            PaymentService paymentService,
            OrderProductRepository orderProductRepository,
            StockReservationService stockReservationService
    ) {
        this.paymentService = paymentService;
        this.orderProductRepository = orderProductRepository;
        this.stockReservationService = stockReservationService;
    }

    //
    @Transactional
    public PaymentResponse createPayment(
            Long memberId,
            Long orderId,
            PaymentRequest request
    ) {
        //기존 서비스에서 주문 소유자 상태 검증 및 결제 생성
        PaymentResponse response = paymentService.createPayment(memberId, orderId, request);

        //DB에 저장된 주문 상품으로 예약 목록 생성
        List<ReservationItem> items = orderProductRepository.findByOrder_OrderId(orderId)
                .stream()
                .map(item -> new ReservationItem(
                        item.getProduct().getProductId(),
                        item.getAmount()
                )).toList();

        if (items.isEmpty()) {
            throw new IllegalStateException("예약할 주문 상품이 없습니다.");
        }

        Long paymentId = response.getPayId();
        // Redis 호출 전에 DB 롧백 시 정리할 작업 등록
        registerRollbackCleanup(paymentId);

        //Redis 예약
        stockReservationService.reserve(paymentId, items);
        return response;
    }

    //DB 롤백 시 정리 작업
    private void registerRollbackCleanup(Long paymentId){
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK){
                            releaseAfterRollback(paymentId);
                        }
                        else if(status == STATUS_UNKNOWN) {
                            // 커밋되었을 수도 있으므로 임의로 반환하지 않는다.
                            log.error(
                                    "결제 트랜잭션 결과 확인 필요: paymentId={}", paymentId
                            );
                        }
                    }
                }
        );
    }
    private void releaseAfterRollback(Long paymentId){
        try {
            stockReservationService.release(paymentId);
        } catch (Exception e) {
            // 정리 실패가 원래 결제 생성 오류를 덮어쓰지 않도록 기록
            log.error(
                    "롤백 후 예약 정리 확인 필요: paymentId={}", paymentId, e
            );
        }
    }

}

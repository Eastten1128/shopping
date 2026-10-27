package com.jdr1128.shopping.domain.payment.dto;

import com.jdr1128.shopping.domain.order.entity.OrderStatus;
import com.jdr1128.shopping.domain.payment.entity.Payment;
import com.jdr1128.shopping.domain.payment.entity.PaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentResponse {

    private Long payId;
    private Long orderId;
    private int payPrice;
    private String cardCompany;
    private String cardNumber;
    private PaymentStatus payStatus;
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;

    public PaymentResponse(
            Long payId,
            Long orderId,
            int payPrice,
            String cardCompany,
            String cardNumber,
            PaymentStatus payStatus,
            OrderStatus orderStatus,
            LocalDateTime createdAt
    ) {
        this.payId = payId;
        this.orderId = orderId;
        this.payPrice = payPrice;
        this.cardCompany = cardCompany;
        this.cardNumber = cardNumber;
        this.payStatus = payStatus;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
    }

    public static PaymentResponse from(Payment payment){
        return new PaymentResponse(
                payment.getPayId(),
                payment.getOrder().getOrderId(),
                payment.getPayPrice(),
                payment.getCardCompany(),
                payment.getCardNumber(),
                payment.getPayStatus(),
                payment.getOrder().getOrderStatus(),
                payment.getCreatedAt()
        );
    }


}

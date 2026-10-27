package com.jdr1128.shopping.domain.payment.entity;

import com.jdr1128.shopping.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name="payments")
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pay_id")
    private Long payId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="order_id", nullable = false, unique = true)
    private Order order;

    @Column(name="card_company")
    private String cardCompany;

    @Column(name="card_number")
    private String cardNumber;

    @Column(name="pay_price", nullable = false)
    private int payPrice;

    @CreatedDate
    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_status", nullable = false)
    private PaymentStatus payStatus;

    protected Payment() {}

    public Payment(
            Order order,
            String cardCompany,
            String cardNumber,
            int payPrice
            ) {
        this.order = order;
        this.cardCompany = cardCompany;
        this.cardNumber = cardNumber;
        this.payPrice = payPrice;
        this.payStatus = PaymentStatus.READY;
    }

    public void complete() { this.payStatus = PaymentStatus.COMPLETE;}
    public void fail() {this.payStatus = PaymentStatus.FAILED;}
    public void cancel() {this.payStatus = PaymentStatus.CANCELLED;}

    public boolean isComplete() {return this.payStatus == PaymentStatus.COMPLETE;}
    public boolean isFailed() {return this.payStatus == PaymentStatus.FAILED;}
    public boolean isCancelled() {return this.payStatus == PaymentStatus.CANCELLED;}

}

package com.jdr1128.shopping.domain.order.entity;

import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id", nullable = false)
    private Member member;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name="phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "address1", nullable = false)
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "requested_term")
    private String requestedTerm;

    @Column(name="total_order_price", nullable = false)
    private int totalOrderPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @CreatedDate
    @Column(name="order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @LastModifiedDate
    @Column(name = "updated_order_date")
    private LocalDateTime updateOrderDate;

    protected Order() {

    }

    public Order(
                 Member member,
                 String name,
                 String phoneNumber,
                 String address1,
                 String address2,
                 String requestedTerm,
                 int totalOrderPrice

    ) {

        this.member = member;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address1 = address1;
        this.address2 = address2;
        this.requestedTerm = requestedTerm;
        this.totalOrderPrice = totalOrderPrice;
        this.orderStatus = OrderStatus.ORDERED;
    }

    public void cancel(){
        this.orderStatus = OrderStatus.CANCELLED;
    }
}

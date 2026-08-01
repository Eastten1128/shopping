package com.jdr1128.shopping.domain.order.dto;

import com.jdr1128.shopping.domain.order.entity.Order;
import com.jdr1128.shopping.domain.order.entity.OrderProduct;
import com.jdr1128.shopping.domain.order.entity.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderResponse {

    private Long orderId;
    private Long memberId;
    private String name;
    private String phoneNumber;
    private String address1;
    private String address2;
    private String requestedTerm;
    private int totalOrderPrice;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
    private LocalDateTime updateOrderDate;
    private List<OrderProductResponse> orderProducts;

    public OrderResponse(
            Long orderId,
            Long memberId,
            String name,
            String phoneNumber,
            String address1,
            String address2,
            String requestedTerm,
            int totalOrderPrice,
            OrderStatus orderStatus,
            LocalDateTime orderDate,
            LocalDateTime updateOrderDate,
            List<OrderProductResponse> orderProducts
    ) {
        this.orderId = orderId;
        this.memberId = memberId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address1 = address1;
        this.address2 = address2;
        this.requestedTerm = requestedTerm;
        this.totalOrderPrice = totalOrderPrice;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
        this.updateOrderDate = updateOrderDate;
        this.orderProducts = orderProducts;
    }

    public static OrderResponse of(
            Order order,
            List<OrderProduct> orderProducts
    ) {
        List<OrderProductResponse> orderProductResponses = orderProducts.stream()
                .map(OrderProductResponse::from)
                .toList();

        return new OrderResponse(
                order.getOrderId(),
                order.getMember().getMemberId(),
                order.getName(),
                order.getPhoneNumber(),
                order.getAddress1(),
                order.getAddress2(),
                order.getRequestedTerm(),
                order.getTotalOrderPrice(),
                order.getOrderStatus(),
                order.getOrderDate(),
                order.getUpdateOrderDate(),
                orderProductResponses
        );
    }
}

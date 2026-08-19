package com.jdr1128.shopping.domain.order.dto;

import com.jdr1128.shopping.domain.order.entity.OrderProduct;
import lombok.Getter;

@Getter
public class OrderProductResponse {

    private Long orderProductId;
    private Long productId;
    private String productName;
    private int amount;
    private int price;
    private int orderPrice;

    public OrderProductResponse(
            Long orderProductId,
            Long productId,
            String productName,
            int amount,
            int price,
            int orderPrice
    ) {
        this.orderProductId = orderProductId;
        this.productId = productId;
        this.productName = productName;
        this.amount = amount;
        this.price = price;
        this.orderPrice = price * amount;
    }

    public static OrderProductResponse from(OrderProduct orderProduct){
        return new OrderProductResponse(
                orderProduct.getOrderProductId(),
                orderProduct.getProduct().getProductId(),
                orderProduct.getProductName(),
                orderProduct.getAmount(),
                orderProduct.getPrice(),
                orderProduct.getOrderPrice()
        );
    }


}

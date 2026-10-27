package com.jdr1128.shopping.domain.order.entity;

import com.jdr1128.shopping.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_products")
@Getter
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_product_id")
    private Long orderProductId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name="product_name", nullable = false)
    private String productName;

    @Column(name="amount", nullable = false)
    private int amount;

    @Column(name="price", nullable = false)
    private int price;

    @Column(name="order_price", nullable = false)
    private int orderPrice;

    protected OrderProduct(){}

    public OrderProduct(
            Order order,
            Product product,
            String productName,
            int amount,
            int price
    ) {
        this.order = order;
        this.product = product;
        this.productName = productName;
        this.amount = amount;
        this.price = price;
        this.orderPrice = price * amount;
    }
}

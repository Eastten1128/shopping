package com.jdr1128.shopping.domain.order.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderProductTest {

    @Test
    void storesOrderPriceSnapshotWhenCreated() {
        OrderProduct orderProduct = new OrderProduct(null, null, "테스트 상품", 3, 30000);

        assertThat(orderProduct.getAmount()).isEqualTo(3);
        assertThat(orderProduct.getPrice()).isEqualTo(30000);
        assertThat(orderProduct.getOrderPrice()).isEqualTo(90000);
    }
}

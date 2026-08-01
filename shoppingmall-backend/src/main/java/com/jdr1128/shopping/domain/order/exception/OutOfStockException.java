package com.jdr1128.shopping.domain.order.exception;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException() {
        super("상품 재고가 부족하여 주문을 실행할 수 없습니다.");
    }

}

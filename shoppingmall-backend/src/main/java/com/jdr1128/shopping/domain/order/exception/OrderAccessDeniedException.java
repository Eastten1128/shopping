package com.jdr1128.shopping.domain.order.exception;

public class OrderAccessDeniedException extends RuntimeException {

    public OrderAccessDeniedException() {
        super("해당 주문에 접근할 권한이 없습니다.");
    }
}

package com.jdr1128.shopping.domain.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("장바구니가 비어 있어 주문을 생성할 수 없습니다.");
    }
}

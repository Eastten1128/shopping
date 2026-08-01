package com.jdr1128.shopping.domain.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProductNotAvailableException extends RuntimeException {
    public ProductNotAvailableException() {
        super("장바구니에 담을 수 없는 상품입니다.");
    }
}

package com.jdr1128.shopping.domain.order.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException() {
        //super("주문을 찾을 수 없습니다.");
        super(ErrorCode.ORDER_NOT_FOUND);

    }
}

package com.jdr1128.shopping.domain.order.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;

public class OrderAccessDeniedException extends BusinessException {

    public OrderAccessDeniedException() {
        //super("해당 주문에 접근할 권한이 없습니다.");
        super(ErrorCode.ORDER_ACCESS_DENIED);
    }
}

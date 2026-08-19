package com.jdr1128.shopping.domain.order.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;

public class OutOfStockException extends BusinessException {
    public OutOfStockException() {
        //super("상품 재고가 부족하여 주문을 실행할 수 없습니다.");
        super(ErrorCode.OUT_OF_STOCK);
    }

}

package com.jdr1128.shopping.domain.payment.exception;


import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;

public class AlreadyPaidException extends BusinessException {
    public AlreadyPaidException() {
        //super("이미 결제가 처리된 주문입니다.");
        super(ErrorCode.ALREADY_PAID);
    }
}

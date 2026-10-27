package com.jdr1128.shopping.domain.payment.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.CONFLICT)
public class PaymentCancelException extends BusinessException {

    public PaymentCancelException() {
        //super("취소할 수 없는 결제입니다.");
        super(ErrorCode.PAYMENT_CANCEL_NOT_ALLOWED);
    }
}

package com.jdr1128.shopping.domain.payment.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.NOT_FOUND)
public class PaymentNotFoundException extends BusinessException {

    public PaymentNotFoundException() {

        //super("결제 정보를 찾을 수 없습니다.");
        super(ErrorCode.PAYMENT_NOT_FOUND);
    }
}

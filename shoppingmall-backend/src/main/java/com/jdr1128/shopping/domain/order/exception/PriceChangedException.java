package com.jdr1128.shopping.domain.order.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.CONFLICT)
public class PriceChangedException extends BusinessException {
    public PriceChangedException() {
        //super("가격이 변경되었습니다.");
        super(ErrorCode.PRICE_CHANGED);
    }
}

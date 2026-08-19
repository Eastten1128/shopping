package com.jdr1128.shopping.domain.product.expection;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
    public class ProductNotAvailableException extends BusinessException {
        public ProductNotAvailableException() {
            //super("장바구니에 담을 수 없는 상품입니다.");
            super(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }
    }

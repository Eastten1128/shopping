package com.jdr1128.shopping.domain.product.expection;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException() {
        //super("상품을 찾을 수 없습니다.");
        super(ErrorCode.PRODUCT_NOT_FOUND);
    }
}

package com.jdr1128.shopping.domain.product.expection;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;

public class ProductAccessDeniedException extends BusinessException {
    public ProductAccessDeniedException() {
        //super("해당 상품을 수정하거나 삭제할 권한이 없습니다.");
        super(ErrorCode.PRODUCT_ACCESS_DENIED);
    }
}

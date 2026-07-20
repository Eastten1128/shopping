package com.jdr1128.shopping.domain.product.expection;

public class ProductAccessDeniedException extends RuntimeException {
    public ProductAccessDeniedException() {
        super("해당 상품을 수정하거나 삭제할 권한이 없습니다.");
    }
}

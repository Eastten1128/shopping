package com.jdr1128.shopping.domain.category.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.NOT_FOUND)
public class CategoryNotFoundException extends BusinessException {

    public CategoryNotFoundException() {
        //super("카테고리를 찾을 수 없습니다.");
        super(ErrorCode.CATEGORY_NOT_FOUND);
    }
}

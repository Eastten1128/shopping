package com.jdr1128.shopping.domain.category.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateCategoryNameException extends BusinessException {

    public DuplicateCategoryNameException() {
        //super("이미 존재하는 카테고리명입니다.");
        super(ErrorCode.DUPLICATE_CATEGORY_NAME);
    }
}

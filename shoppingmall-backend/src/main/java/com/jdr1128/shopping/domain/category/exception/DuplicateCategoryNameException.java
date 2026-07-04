package com.jdr1128.shopping.domain.category.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateCategoryNameException extends RuntimeException {

    public DuplicateCategoryNameException() {
        super("이미 존재하는 카테고리명입니다.");
    }
}

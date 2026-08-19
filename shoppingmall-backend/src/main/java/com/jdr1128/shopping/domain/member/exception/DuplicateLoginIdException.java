package com.jdr1128.shopping.domain.member.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;

public class DuplicateLoginIdException extends BusinessException {

    public DuplicateLoginIdException() {
        //super("이미 사용 중인 로그인 ID 입니다.");
        super(ErrorCode.DUPLICATE_LOGIN_ID);
    }
}

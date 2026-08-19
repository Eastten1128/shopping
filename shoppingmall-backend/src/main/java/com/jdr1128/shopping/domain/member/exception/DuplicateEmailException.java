package com.jdr1128.shopping.domain.member.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;

public class DuplicateEmailException extends BusinessException {
    public DuplicateEmailException() {
        //super("이미 사용 중인 이메일입니다.");
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}

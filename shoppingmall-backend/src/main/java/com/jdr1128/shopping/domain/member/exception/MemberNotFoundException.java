package com.jdr1128.shopping.domain.member.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;

public class MemberNotFoundException extends BusinessException {
    public MemberNotFoundException()
    {
        super(ErrorCode.MEMBER_NOT_FOUND);
    }
}

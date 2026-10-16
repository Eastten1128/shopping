package com.jdr1128.shopping.domain.auth.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;

public class InvalidRefreshTokenException extends BusinessException {
    public InvalidRefreshTokenException() {

        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}

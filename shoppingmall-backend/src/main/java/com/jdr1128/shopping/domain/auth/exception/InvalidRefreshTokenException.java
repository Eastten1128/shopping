package com.jdr1128.shopping.domain.auth.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;

public class InvalidRefreshTokenError extends BusinessException {
    public InvalidRefreshTokenError() {

        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}

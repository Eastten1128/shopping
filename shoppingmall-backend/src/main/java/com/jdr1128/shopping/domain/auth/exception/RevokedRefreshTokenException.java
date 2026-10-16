package com.jdr1128.shopping.domain.auth.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;

public class RevokedRefreshTokenException extends BusinessException {
    public RevokedRefreshTokenException() {
        super(ErrorCode.REVOKED_REFRESH_TOKEN);
    }
}

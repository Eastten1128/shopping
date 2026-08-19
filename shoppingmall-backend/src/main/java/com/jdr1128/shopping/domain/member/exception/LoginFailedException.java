package com.jdr1128.shopping.domain.member.exception;

import com.jdr1128.shopping.global.exception.BusinessException;
import com.jdr1128.shopping.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class LoginFailedException extends BusinessException {

    public LoginFailedException(){
        //super("로그인 ID 또는 비밀번호가 올바르지 않습니다");
        super(ErrorCode.LOGIN_FAILED);
    }
}

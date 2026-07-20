package com.jdr1128.shopping.domain.member.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class LoginFailedException extends RuntimeException{

    public LoginFailedException(){
        super("로그인 ID 또는 비밀번호가 올바르지 않습니다");
    }
}

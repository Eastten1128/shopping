package com.jdr1128.shopping.domain.member.exception;

public class DuplicateLoginIdException extends RuntimeException{

    public DuplicateLoginIdException() {
        super("이미 사용 중인 로그인 ID 입니다.");
    }
}

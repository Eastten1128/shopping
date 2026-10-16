package com.jdr1128.shopping.domain.member.dto;

//내부 전달용 DTO
// Controller에서 Cookie를 만들기 위해

import lombok.Getter;

@Getter
public class LoginResult {

    private final LoginResponse loginResponse;
    private final String refreshToken;

    public LoginResult(
            LoginResponse loginResponse,
            String refreshToken
    ) {
        this.loginResponse = loginResponse;
        this.refreshToken = refreshToken;
    }
}

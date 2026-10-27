package com.jdr1128.shopping.domain.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;


@Getter
public class LoginRequest {

    @NotNull(message = "로그인 ID는 필수입니다.")
    private String loginId;

    @NotNull(message = "비밀번호는 필수입니다.")
    private String password;
}

package com.jdr1128.shopping.domain.member.dto;

import com.jdr1128.shopping.domain.member.entity.Role;
import lombok.Getter;

@Getter
public class LoginResponse {

    private String tokenType;
    private String accessToken;
    private Long memberId;
    private String loginId;
    private Role role;

    public LoginResponse(String tokenType,
                         String accessToken,
                         Long memberId,
                         String loginId,
                         Role role
    ) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.memberId = memberId;
        this.loginId = loginId;
        this.role = role;
    }

    public static LoginResponse of(
            String accessToken,
            Long memberId,
            String loginId,
            Role role
    ){
        return new LoginResponse(
                "Bearer",
                accessToken,
                memberId,
                loginId,
                role
        );
    }
}

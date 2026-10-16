package com.jdr1128.shopping.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieProvider {

    private final String cookieName;
    private final String cookiePath;
    private final long maxAgeSeconds;
    private final boolean secure;
    private final String sameSite;


    public RefreshTokenCookieProvider(
            @Value("${jwt.refresh-cookie-name}") String cookieName,
            @Value("${jwt.refresh-cookie-path}") String cookiePath,
            @Value("${jwt.refresh-cookie-max-age-seconds}") long maxAgeSeconds,
            @Value("${jwt.refresh-cookie-secure}") boolean secure,
            @Value("${jwt.refresh-cookie-same-site}") String sameSite
    ) {
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.maxAgeSeconds = maxAgeSeconds;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    //로그인 성공 시 사용할 Cookie 생성
    //최종 응답 Header에 Cookie 정보가 들어감
    public ResponseCookie createRefreshTokenCookie(String refreshToken){
        return ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true) //Javascript에서 쿠키를 직접 읽지 못하게 한다.
                .secure(secure) // HTTPS와 같은 보안 연결을 통해서만 쿠키가 전송되도록 제한
                .sameSite(sameSite) // 서로 다른 도메인간 요청을 보낼 때 쿠키를 같이 보낼 지
                .path(cookiePath)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    //로그아웃 시 maxAge를 0으로 만들어서 브라우저에서 쿠키를 삭제시킨다.
    public ResponseCookie createExpiredRefreshTokenCookie(String refreshToken){
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(0)
                .build();
    }
}

package com.jdr1128.shopping.global.security;

import com.jdr1128.shopping.domain.member.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 전용 유틸 클래스
 * 1. JWT 생성
 * 2. JWT 검증
 * 3. JWT에서 loginId 추출
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-milliseconds}") long expirationMilliseconds
    ){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMilliseconds = expirationMilliseconds;
    }

    //JWT 토큰 생성
    public String createAccessToken(String loginId, Role role){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMilliseconds);

        return Jwts.builder()
                .subject(loginId)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    //토큰에서 로그인 ID를 꺼내는 메서드
    public String getLoginId(String token){
        Claims claims = parseClaims(token);

        return claims.getSubject();
    }

    //토큰이 정상인지 체크
    //1. 토큰 형식이 올바른가?
    //2. 서명이 올바른가?
    //3. 만료되지 않았는가?
    public boolean validateToken(String token){
        try{
            parseClaims(token);
            return true;
        }   catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }

    //JWT를 파싱
    //token 문자열 -> secretKey로 서명 검증 -> Claims 추출
    private Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

package com.jdr1128.shopping.global.security;

import com.jdr1128.shopping.domain.member.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


//Access Token과, RefreshToken을 만들고 검증하는 역할을 수행한다.
@Component
public class JwtTokenProvider {

    //상수 생성
    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    //
    private final SecretKey secretKey;
    private final long accessTokenExpirationMilliseconds;
    private final long refreshTokenExpirationMilliseconds;

    //생성자 설정값 주입
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-milliseconds}") long accessTokenExpirationMilliseconds,
            @Value("${jwt.refresh-token-expiration-milliseconds}") long refreshTokenExpirationMilliseconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMilliseconds = accessTokenExpirationMilliseconds;
        this.refreshTokenExpirationMilliseconds = refreshTokenExpirationMilliseconds;
    }

    //AccessToken 생성
    public String createAccessToken(
            Long memberId,
            String loginId,
            Role role
    ) {
        return createToken(
                memberId,
                loginId,
                role,
                TokenType.ACCESS,
                accessTokenExpirationMilliseconds
        );
    }

    /*
     * 기존 MemberService 코드가 아직 createAccessToken(String loginId, Role role)을
     * 호출하고 있을 수 있으므로 임시로 유지합니다.
     *
     * 6.5-5 단계에서 MemberService를 수정하면
     * createAccessToken(memberId, loginId, role) 형태를 사용하게 됩니다.
     */
    public String createAccessToken(
            String loginId,
            Role role
    ) {
        return createToken(
                null,
                loginId,
                role,
                TokenType.ACCESS,
                accessTokenExpirationMilliseconds
        );
    }

    //Refresh Token 생성
    //Access Token을 재발급할 때 DB에서 회원을 다시 조회해서 최신 권한을 반영하는 것이 더 안정적
    public String createRefreshToken(
            Long memberId,
            String loginId
    ) {
        return createToken(
                memberId,
                loginId,
                null,
                TokenType.REFRESH,
                refreshTokenExpirationMilliseconds
        );
    }

    //AccessToken 검증
    public boolean validateAccessToken(String token) {
        return validateToken(token, TokenType.ACCESS);
    }

    //RefreshToken 검증
    public boolean validateRefreshToken(String token) {
        return validateToken(token, TokenType.REFRESH);
    }

    /*
    토큰 검증 기준
    1. JWT 형식이 정상인가?
    2. 서명이 정상인가?
    3. 만료되지 않았는가?
    4. tokenType 검사(ACCESS | REFRESH)
 */
    public boolean validateToken(
            String token,
            TokenType expectedTokenType
    ) {
        try {
            //토큰 복호화
            Claims claims = parseClaims(token);
            //토큰 타입 읽기 (ACCESS | REFRESH)
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            //Token이 유효한지 검사
            return expectedTokenType.name().equals(tokenType);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getMemberId(String token) {
        Claims claims = parseClaims(token);
        Object memberId = claims.get(MEMBER_ID_CLAIM);

        if (memberId instanceof Number number) {
            return number.longValue();
        }

        throw new IllegalArgumentException("JWT에 memberId가 없습니다.");
    }

    /**
     * 각 Claim 객체에서 다음을 꺼내온다
     * loginId
     * Role
     * TokenType
     * Expiration
     */
    public String getLoginId(String token) {
        Claims claims = parseClaims(token);

        return claims.getSubject();
    }

    public Role getRole(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get(ROLE_CLAIM, String.class);

        return Role.valueOf(role);
    }

    public TokenType getTokenType(String token) {
        Claims claims = parseClaims(token);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        return TokenType.valueOf(tokenType);
    }

    public LocalDateTime getExpiration(String token) {
        Claims claims = parseClaims(token);

        return LocalDateTime.ofInstant(
                claims.getExpiration().toInstant(),
                ZoneId.systemDefault()
        );
    }

    //새 토큰을 생성한다
    private String createToken(
            Long memberId,
            String loginId,
            Role role,
            TokenType tokenType,
            long expirationMilliseconds
    ) {
        //만료시간 = 현재시간 + 설정만료시간
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMilliseconds);

        //
        JwtBuilder builder = Jwts.builder()
                .subject(loginId)
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey);

        if (tokenType == TokenType.REFRESH) {
            builder.id(UUID.randomUUID().toString());
        }

        if (memberId != null) {
            builder.claim(MEMBER_ID_CLAIM, memberId);
        }

        if (role != null) {
            builder.claim(ROLE_CLAIM, role.name());
        }

        return builder.compact();
    }



    //JWT를 해석하는 메서드
    /*
    * 1. secreKey로 서명 검증
    * 2. 만료여부 검증
    * 3. payload 추출
    * */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

package com.jdr1128.shopping.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.jdr1128.shopping.domain.member.entity.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            "REMOVED_JWT_SECRET",
            1800000L,
            604800000L
    );

    @Test
    @DisplayName("같은 회원의 Refresh Token을 연속 발급해도 서로 다른 값이다")
    void refreshTokensAreUniquePerIssuance() {
        String first = jwtTokenProvider.createRefreshToken(1L, "user01");
        String second = jwtTokenProvider.createRefreshToken(1L, "user01");

        assertThat(second).isNotEqualTo(first);
        assertThat(jwtTokenProvider.validateRefreshToken(first)).isTrue();
        assertThat(jwtTokenProvider.validateRefreshToken(second)).isTrue();
    }

    @Test
    @DisplayName("Access Token을 생성하고 Access Token으로 검증할 수 있다")
    void createAndValidateAccessToken() {
        // given
        String accessToken = jwtTokenProvider.createAccessToken(
                1L,
                "user01",
                Role.USER
        );

        // when
        boolean accessValid = jwtTokenProvider.validateAccessToken(accessToken);
        boolean refreshValid = jwtTokenProvider.validateRefreshToken(accessToken);

        // then
        assertThat(accessValid).isTrue();
        assertThat(refreshValid).isFalse();

        assertThat(jwtTokenProvider.getMemberId(accessToken)).isEqualTo(1L);
        assertThat(jwtTokenProvider.getLoginId(accessToken)).isEqualTo("user01");
        assertThat(jwtTokenProvider.getRole(accessToken)).isEqualTo(Role.USER);
        assertThat(jwtTokenProvider.getTokenType(accessToken)).isEqualTo(TokenType.ACCESS);
        assertThat(jwtTokenProvider.getExpiration(accessToken)).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Refresh Token을 생성하고 Refresh Token으로 검증할 수 있다")
    void createAndValidateRefreshToken() {
        // given
        String refreshToken = jwtTokenProvider.createRefreshToken(
                1L,
                "user01"
        );

        // when
        boolean accessValid = jwtTokenProvider.validateAccessToken(refreshToken);
        boolean refreshValid = jwtTokenProvider.validateRefreshToken(refreshToken);

        // then
        assertThat(accessValid).isFalse();
        assertThat(refreshValid).isTrue();

        assertThat(jwtTokenProvider.getMemberId(refreshToken)).isEqualTo(1L);
        assertThat(jwtTokenProvider.getLoginId(refreshToken)).isEqualTo("user01");
        assertThat(jwtTokenProvider.getTokenType(refreshToken)).isEqualTo(TokenType.REFRESH);
        assertThat(jwtTokenProvider.getExpiration(refreshToken)).isAfter(LocalDateTime.now());
    }
}

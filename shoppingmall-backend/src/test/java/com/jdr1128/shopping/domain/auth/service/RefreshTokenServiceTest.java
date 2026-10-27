package com.jdr1128.shopping.domain.auth.service;

import com.jdr1128.shopping.domain.auth.entity.RefreshToken;
import com.jdr1128.shopping.domain.auth.exception.InvalidRefreshTokenException;
import com.jdr1128.shopping.domain.auth.exception.RefreshTokenNotFoundException;
import com.jdr1128.shopping.domain.auth.exception.RevokedRefreshTokenException;
import com.jdr1128.shopping.domain.auth.repository.RefreshTokenRepository;
import com.jdr1128.shopping.domain.member.dto.LoginResult;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import com.jdr1128.shopping.global.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:refresh-token-service;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RefreshTokenService.class, RefreshTokenServiceTest.TokenConfiguration.class})
class RefreshTokenServiceTest {

    @Autowired private RefreshTokenService service;
    @Autowired private RefreshTokenRepository tokens;
    @Autowired private MemberRepository members;
    @Autowired private JwtTokenProvider jwt;
    @Autowired private EntityManager entityManager;

    @Test
    void loginReplacesOldRowsAndStoresOnlyHash() {
        Member member = saveMember("login");
        Member other = saveMember("other");
        String first = service.issueRefreshToken(member);
        service.issueRefreshToken(other);
        Long oldId = tokens.findAll().stream()
                .filter(token -> token.getMember().getMemberId().equals(member.getMemberId()))
                .findFirst().orElseThrow().getRefreshTokenId();

        String replacement = service.issueRefreshToken(member);
        entityManager.flush();
        entityManager.clear();

        assertThat(replacement).isNotEqualTo(first);
        assertThat(tokens.findById(oldId)).isEmpty();
        assertThat(tokens.findAll()).hasSize(2).allSatisfy(token -> {
            assertThat(token.getTokenHash()).hasSize(64).isNotEqualTo(replacement);
            assertThat(token.isRevoked()).isFalse();
        });
    }

    @Test
    void rotationDeletesPreviousTokenAndRejectsReplay() {
        Member member = saveMember("rotate");
        String oldToken = service.issueRefreshToken(member);
        Long oldId = tokens.findAll().get(0).getRefreshTokenId();

        LoginResult result = service.rotateRefreshToken(oldToken);
        entityManager.flush();
        entityManager.clear();

        assertThat(tokens.findById(oldId)).isEmpty();
        assertThat(tokens.count()).isEqualTo(1);
        assertThat(result.getRefreshToken()).isNotEqualTo(oldToken);
        assertThat(jwt.validateRefreshToken(result.getRefreshToken())).isTrue();
        assertThat(jwt.validateAccessToken(result.getLoginResponse().getAccessToken())).isTrue();
        assertThat(result.getLoginResponse().getMemberId()).isEqualTo(member.getMemberId());
        assertThatThrownBy(() -> service.rotateRefreshToken(oldToken))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutPhysicallyDeletesTokenAndIsIdempotent() {
        String token = service.issueRefreshToken(saveMember("logout"));
        service.logout(token);
        service.logout(token);
        service.logout(null);
        service.logout(" ");
        entityManager.flush();
        entityManager.clear();

        assertThat(tokens.count()).isZero();
        assertThat(members.count()).isEqualTo(1);
        assertThatThrownBy(() -> service.rotateRefreshToken(token))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsExpiredStoredToken() {
        String token = service.issueRefreshToken(saveMember("expired"));
        RefreshToken stored = tokens.findAll().get(0);
        ReflectionTestUtils.setField(stored, "expiresAt", LocalDateTime.now().minusSeconds(1));
        entityManager.flush();

        assertThatThrownBy(() -> service.rotateRefreshToken(token))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsLegacyRevokedToken() {
        String token = service.issueRefreshToken(saveMember("revoked"));
        ReflectionTestUtils.setField(tokens.findAll().get(0), "revoked", true);
        entityManager.flush();

        assertThatThrownBy(() -> service.rotateRefreshToken(token))
                .isInstanceOf(RevokedRefreshTokenException.class);
    }

    @Test
    void rejectsMissingOrWrongTypeToken() {
        Member member = saveMember("invalid");
        String access = jwt.createAccessToken(member.getMemberId(), member.getLoginId(), member.getRole());
        assertThatThrownBy(() -> service.rotateRefreshToken(null))
                .isInstanceOf(RefreshTokenNotFoundException.class);
        assertThatThrownBy(() -> service.rotateRefreshToken(access))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotateRefreshToken("invalid"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void cleanupRemovesExpiredAndLegacyRevokedRowsWithoutLogin() {
        Member member = saveMember("cleanup");
        tokens.save(new RefreshToken(member, "a".repeat(64), LocalDateTime.now().minusDays(1)));
        RefreshToken revoked = tokens.save(new RefreshToken(member, "b".repeat(64), LocalDateTime.now().plusDays(7)));
        ReflectionTestUtils.setField(revoked, "revoked", true);
        tokens.save(new RefreshToken(member, "c".repeat(64), LocalDateTime.now().plusDays(7)));

        new RefreshTokenCleanupScheduler(service).cleanup();
        entityManager.clear();

        assertThat(tokens.findAll()).extracting(RefreshToken::getTokenHash).containsExactly("c".repeat(64));
        assertThat(members.findById(member.getMemberId())).isPresent();
    }

    private Member saveMember(String loginId) {
        return members.save(new Member(loginId, "encoded-password", loginId + "@test.com", Role.USER));
    }

    @TestConfiguration
    static class TokenConfiguration {
        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return new JwtTokenProvider("test-only-refresh-token-secret-key-must-be-long-enough-123456", 1800000L, 604800000L);
        }
    }
}

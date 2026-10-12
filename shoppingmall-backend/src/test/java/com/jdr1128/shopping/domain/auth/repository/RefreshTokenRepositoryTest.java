package com.jdr1128.shopping.domain.auth.repository;

import com.jdr1128.shopping.domain.auth.entity.RefreshToken;
import com.jdr1128.shopping.domain.member.entity.Member;
import com.jdr1128.shopping.domain.member.entity.Role;
import com.jdr1128.shopping.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:refresh-token-repository;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("RefreshToken을 저장하고 tokenHash로 조회할 수 있다.")
    void saveAndFindTokenHash() {
        //g
        Member member = new Member(
                "test_user99",
                "encoded-password",
                "user99@test.com",
                Role.USER
        );
        Member savedMember = memberRepository.save(member);

        RefreshToken refreshToken = new RefreshToken(
                savedMember,
                "a".repeat(64),
                LocalDateTime.now().plusDays(7)
        );

        refreshTokenRepository.save(refreshToken);

        //when
        RefreshToken foundToken = refreshTokenRepository.findByTokenHash("a".repeat(64))
                .orElseThrow();

        //then
        assertThat(foundToken.getMember().getMemberId())
                .isEqualTo(savedMember.getMemberId());

        assertThat(foundToken.getTokenHash())
                .isEqualTo("a".repeat(64));

        assertThat(foundToken.isRevoked())
                .isFalse();
    }

    @Test
    @DisplayName("회원 토큰 삭제는 다른 회원의 유효한 토큰을 유지한다.")
    void deletesOnlySelectedMembersTokens() {
        Member first = saveMember("first");
        Member second = saveMember("second");
        saveToken(first, "b", LocalDateTime.now().plusDays(7));
        saveToken(first, "c", LocalDateTime.now().minusDays(1));
        saveToken(second, "d", LocalDateTime.now().plusDays(7));

        assertThat(refreshTokenRepository.deleteAllByMemberId(first.getMemberId())).isEqualTo(2);
        entityManager.clear();

        assertThat(refreshTokenRepository.findAll()).extracting(RefreshToken::getTokenHash)
                .containsExactly("d".repeat(64));
    }

    @Test
    @DisplayName("로그아웃 토큰은 물리 삭제하고 반복 삭제는 안전하다.")
    void deletesByHashIdempotently() {
        Member member = saveMember("logout");
        saveToken(member, "e", LocalDateTime.now().plusDays(7));
        saveToken(member, "f", LocalDateTime.now().plusDays(7));

        assertThat(refreshTokenRepository.deleteByTokenHash("e".repeat(64))).isEqualTo(1);
        assertThat(refreshTokenRepository.deleteByTokenHash("e".repeat(64))).isZero();
        entityManager.clear();

        assertThat(refreshTokenRepository.findByTokenHash("e".repeat(64))).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("f".repeat(64))).isPresent();
    }

    @Test
    @DisplayName("만료 경계 및 기존 회수 행만 삭제하고 정상 토큰과 회원을 유지한다.")
    void cleansExpiredAndLegacyRevokedTokens() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 12, 12, 0);
        Member member = saveMember("cleanup");
        saveToken(member, "1", now.minusSeconds(1));
        RefreshToken boundary = saveToken(member, "2", now);
        RefreshToken revoked = saveToken(member, "3", now.plusDays(7));
        ReflectionTestUtils.setField(revoked, "revoked", true);
        saveToken(member, "4", now.plusSeconds(1));

        assertThat(boundary.isExpired(now)).isTrue();
        assertThat(refreshTokenRepository.deleteExpiredAndRevokedTokens(now)).isEqualTo(3);
        entityManager.clear();

        assertThat(refreshTokenRepository.findAll()).extracting(RefreshToken::getTokenHash)
                .containsExactly("4".repeat(64));
        assertThat(memberRepository.findById(member.getMemberId())).isPresent();
    }

    private Member saveMember(String loginId) {
        return memberRepository.save(new Member(loginId, "encoded-password", loginId + "@test.com", Role.USER));
    }

    private RefreshToken saveToken(Member member, String hash, LocalDateTime expiresAt) {
        return refreshTokenRepository.save(new RefreshToken(member, hash.repeat(64), expiresAt));
    }

}

package com.jdr1128.shopping.domain.auth.repository;

import com.jdr1128.shopping.domain.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    //@Lock: 해당 어노테이션이 담겨있는 Token을 조회할 때 해당 row를 잠근다.
    //동시에 같은 Refresh Token으로 요청이 2개 들어올 수 있기 때문에 요청이 들어오는 즉시 잠궈버린다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rt
            from RefreshToken rt
            join fetch rt.member
            where rt.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken rt where rt.member.memberId = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);

    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken rt where rt.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    // 기존 소프트 삭제 데이터도 함께 정리한다. 유효한 토큰은 남긴다.
    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken rt where rt.expiresAt <= :now or rt.revoked = true")
    int deleteExpiredAndRevokedTokens(@Param("now") LocalDateTime now);
}

package com.jdr1128.shopping.domain.auth.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenCleanupScheduler(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    // 서버 시작 후 한 번 실행하고, 기본 1시간 간격으로 만료/기존 회수 행을 삭제한다.
    @Scheduled(
            fixedDelayString = "${jwt.refresh-token-cleanup-delay-milliseconds:3600000}",
            initialDelayString = "${jwt.refresh-token-cleanup-initial-delay-milliseconds:0}"
    )
    public void cleanup() {
        refreshTokenService.deleteExpiredAndRevokedTokens();
    }
}

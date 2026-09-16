package com.sunday.services.service.impl;

import com.sunday.services.repository.EmailVerificationTokenRepository;
import com.sunday.services.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Purges expired refresh and email-verification tokens so both tables don't grow
 * unbounded (a refresh token is inserted on every login/refresh and never deleted otherwise).
 */
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Scheduled(cron = "${auth.token-cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteByExpiresAtBefore(now);
        emailVerificationTokenRepository.deleteByExpiresAtBefore(now);
    }
}

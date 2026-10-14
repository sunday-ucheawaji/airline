package com.sunday.services.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Limits the emails a signup with an existing account's address can trigger, so nobody can flood an inbox by
 * signing up with someone else's email. One per user per cooldown, shared by every user-service instance.
 */
@Slf4j
@Component
public class SignupNoticeThrottle {

    public static final String KEY_PREFIX = "auth:signup-notice:";

    private final StringRedisTemplate redis;
    private final Duration cooldown;

    @Autowired
    public SignupNoticeThrottle(StringRedisTemplate redis, @Value("${auth.signup-notice-cooldown-minutes:15}") long cooldownMinutes) {
        this(redis, Duration.ofMinutes(cooldownMinutes));
    }

    SignupNoticeThrottle(StringRedisTemplate redis, Duration cooldown) {
        this.redis = redis;
        this.cooldown = cooldown;
    }

    /** True when an email may be sent now. Fails open: an unreachable Redis must not stop the owner hearing about it. */
    public boolean tryAcquire(UUID userId) {
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(KEY_PREFIX + userId, "1", cooldown));
        } catch (RuntimeException e) {
            log.warn("Signup notice throttle unavailable for user {}; sending anyway", userId, e);
            return true;
        }
    }
}

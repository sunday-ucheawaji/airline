package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Makes every access token already issued to a user invalid, by writing "tokens issued before now are revoked" to
 * the Redis the api-gateway checks on every request. The entry only needs to outlive the longest-lived access
 * token, so it expires one minute after the access-token TTL. The key format is mirrored in the gateway.
 */
@Slf4j
@Component
public class AccessTokenRevoker {

    public static final String KEY_PREFIX = "auth:tokens-revoked-before:";

    private final StringRedisTemplate redis;
    private final Duration retention;
    private final Clock clock;

    @Autowired
    public AccessTokenRevoker(StringRedisTemplate redis, @Value("${jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
        this(redis, Duration.ofMinutes(accessTokenTtlMinutes + 1), Clock.systemUTC());
    }

    AccessTokenRevoker(StringRedisTemplate redis, Duration retention, Clock clock) {
        this.redis = redis;
        this.retention = retention;
        this.clock = clock;
    }

    /** Fails closed: if the revocation cannot be recorded, the change that needed it is refused. */
    public void revokeAll(UUID userId) {
        try {
            redis.opsForValue().set(KEY_PREFIX + userId, String.valueOf(clock.instant().getEpochSecond()), retention);
        } catch (RuntimeException e) {
            throw new ServiceUnavailableException(ErrorMessageUtil.ACCESS_REVOCATION_UNAVAILABLE, e);
        }
    }

    /** For logout and refresh-token reuse, where the refresh-token revocation alone must still go through. */
    public void revokeAllQuietly(UUID userId) {
        try {
            revokeAll(userId);
        } catch (ServiceUnavailableException e) {
            log.warn("Could not revoke access tokens of user {}; they expire on their own", userId, e);
        }
    }
}

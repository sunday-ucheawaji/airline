package com.sunday.cloud.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Rejects an access token issued before its user's revocation cutoff. user-service writes the cutoff on logout,
 * suspension, password change or reset, platform-role changes and refresh-token reuse.
 *
 * <p>Fails open: if Redis cannot be read the token is accepted (and the outage logged). Access tokens expire within
 * minutes anyway, and failing closed would lock every user out of the platform for the length of a Redis outage.
 */
@Slf4j
@Component
public class AccessTokenRevocationCheck {

    /** Mirrors user-service's AccessTokenRevoker; the gateway does not depend on that service's code. */
    static final String KEY_PREFIX = "auth:tokens-revoked-before:";

    private final ReactiveStringRedisTemplate redis;

    public AccessTokenRevocationCheck(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    public Mono<Jwt> check(Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        Instant issuedAt = jwt.getIssuedAt();
        if (userId == null || issuedAt == null) {
            return Mono.just(jwt);
        }
        return redis.opsForValue().get(KEY_PREFIX + userId)
                .onErrorResume(e -> {
                    log.warn("Could not read the access-token revocation of user {}; accepting the token", userId, e);
                    return Mono.empty();
                })
                .flatMap(cutoff -> issuedAt.getEpochSecond() < Long.parseLong(cutoff)
                        ? Mono.<Jwt>error(new BadJwtException("Token has been revoked"))
                        : Mono.just(jwt))
                .defaultIfEmpty(jwt);
    }
}

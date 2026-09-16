package com.sunday.cloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String PREFIX = "jwt:blacklist:";

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Void> blacklist(String token, Duration ttl) {

        if (ttl.isNegative() || ttl.isZero()) {
            return Mono.empty();
        }

        return redisTemplate
                .opsForValue()
                .set(
                        PREFIX + token,
                        "1",
                        ttl
                )
                .doOnSuccess(result ->
                        log.debug(
                                "Token blacklisted for {}s",
                                ttl.toSeconds()
                        )
                )
                .then();
    }

    public Mono<Boolean> isBlacklisted(String token) {

        return redisTemplate
                .hasKey(PREFIX + token)
                .onErrorResume(error -> {

                    log.warn(
                            "Redis unavailable for blacklist check — treating token as valid: {}",
                            error.getMessage()
                    );

                    return Mono.just(false);
                });
    }
}
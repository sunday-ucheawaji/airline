package com.sunday.services.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookie {

    public static final String NAME = "refresh_token";
    private static final String PATH = "/auth";

    private final Duration maxAge;
    private final boolean secure;

    public RefreshTokenCookie(
            @Value("${auth.refresh-token-ttl-days}") long refreshTokenTtlDays,
            @Value("${auth.refresh-cookie.secure:true}") boolean secure) {
        this.maxAge = Duration.ofDays(refreshTokenTtlDays);
        this.secure = secure;
    }

    public String issue(String refreshToken) {
        return build(refreshToken, maxAge);
    }

    public String clear() {
        return build("", Duration.ZERO);
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}

package com.sunday.services.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.sunday.services.service.impl.CustomUserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues access tokens. The signing algorithm is pinned to HS256 explicitly — jjwt would
 * otherwise infer it from the key length (a 64-char secret silently becomes HS512), and the
 * api-gateway's decoder pins HS256 too, so both sides must agree on it rather than on a side effect.
 */
@Service
public class JwtProvider {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final String issuer;

    @Value("${jwt.access-token-ttl-minutes}")
    private long accessTokenTtlMinutes;

    public JwtProvider(@Value("${jwt.secret}") String secret, @Value("${jwt.issuer}") String issuer) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = issuer;
    }

    /**
     * Roles and permissions travel as two separate JSON-array claims: {@code roles} and {@code permissions}.
     * The user's authorities are split by the {@code ROLE_} prefix: prefixed ones are roles (prefix removed), the
     * rest are permissions. That is unambiguous because no permission name starts with {@code ROLE_}.
     */
    public String generateToken(UserDetails user, Long userId) {
        List<String> roles = new ArrayList<>();
        List<String> permissions = new ArrayList<>();
        for (GrantedAuthority authority : user.getAuthorities()) {
            String name = authority.getAuthority();
            if (name.startsWith(CustomUserDetailsService.ROLE_PREFIX)) {
                roles.add(name.substring(CustomUserDetailsService.ROLE_PREFIX.length()));
            } else {
                permissions.add(name);
            }
        }
        Collections.sort(roles);
        Collections.sort(permissions);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenTtlMinutes * 60 * 1000))
                .claim("email", user.getUsername())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("userId", userId)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}

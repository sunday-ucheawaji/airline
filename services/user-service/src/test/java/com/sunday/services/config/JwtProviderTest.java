package com.sunday.services.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";
    private static final String ISSUER = "gds-user-service";

    private final JwtProvider provider = provider();

    @Test
    void authoritiesAreSplitIntoSeparateRolesAndPermissionsClaims() {
        String token = provider.generateToken(user("ROLE_SENIOR_APPROVER", "ONBOARDING_FINAL_APPROVE", "ONBOARDING_APPLICATION_READ"), 7L);

        Claims claims = parse(token);

        assertThat(claims.get("roles", List.class)).containsExactly("SENIOR_APPROVER");
        assertThat(claims.get("permissions", List.class)).containsExactly("ONBOARDING_APPLICATION_READ", "ONBOARDING_FINAL_APPROVE");
        assertThat(claims).doesNotContainKey("authorities");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getSubject()).isEqualTo("a@x.com");
        assertThat(claims.get("email")).isEqualTo("a@x.com");
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(7L);
    }

    /** Regression: these permission names used to start with ROLE_ and would have been mistaken for roles. */
    @Test
    void permissionsAboutRolesAreNotMistakenForRoles() {
        String token = provider.generateToken(
                user("ROLE_SUPER_ADMIN", "USER_ROLE_ASSIGN", "USER_ROLE_REVOKE", "ACCESS_MANAGE"), 7L);

        Claims claims = parse(token);

        assertThat(claims.get("roles", List.class)).containsExactly("SUPER_ADMIN");
        assertThat(claims.get("permissions", List.class)).containsExactly("ACCESS_MANAGE", "USER_ROLE_ASSIGN", "USER_ROLE_REVOKE");
    }

    @Test
    void aUserWithNoAccessGetsEmptyArraysNotMissingClaims() {
        Claims claims = parse(provider.generateToken(user(), 7L));

        assertThat(claims.get("roles", List.class)).isEmpty();
        assertThat(claims.get("permissions", List.class)).isEmpty();
    }

    private static UserDetails user(String... authorities) {
        return new User("a@x.com", "hash", List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }

    private static Claims parse(String token) {
        return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).build()
                .parseSignedClaims(token).getPayload();
    }

    private static JwtProvider provider() {
        JwtProvider provider = new JwtProvider(SECRET, ISSUER);
        ReflectionTestUtils.setField(provider, "accessTokenTtlMinutes", 15L);
        return provider;
    }
}

package com.sunday.cloud.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityHeadersGlobalFilterTest {

    private final IdentityHeadersGlobalFilter filter = new IdentityHeadersGlobalFilter();

    @Test
    void forgedIdentityHeadersAreReplacedWithTheOnesFromTheVerifiedToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/profile")
                .header("X-User-Id", "1")
                .header("X-User-Email", "forged@example.com")
                .header("X-User-Roles", "ROLE_GDS_ADMIN")
                .build());

        HttpHeaders forwarded = run(exchange, authenticated(42L, "real@example.com", "ROLE_B", "ROLE_A"));

        assertThat(forwarded.get("X-User-Id")).containsExactly("42");
        assertThat(forwarded.get("X-User-Email")).containsExactly("real@example.com");
        assertThat(forwarded.get("X-User-Roles")).containsExactly("ROLE_A,ROLE_B");
    }

    @Test
    void anAnonymousRequestCannotSmuggleAnIdentityThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/login")
                .header("X-User-Id", "1")
                .header("X-User-Email", "forged@example.com")
                .header("X-User-Roles", "ROLE_GDS_ADMIN")
                .build());

        HttpHeaders forwarded = run(exchange, null);

        assertThat(forwarded.containsHeader("X-User-Id")).isFalse();
        assertThat(forwarded.containsHeader("X-User-Email")).isFalse();
        assertThat(forwarded.containsHeader("X-User-Roles")).isFalse();
    }

    @Test
    void aUserWithNoRolesGetsAnEmptyRolesHeaderNotAMissingOne() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/profile").build());

        HttpHeaders forwarded = run(exchange, authenticated(7L, "plain@example.com"));

        assertThat(forwarded.getFirst("X-User-Roles")).isEmpty();
    }

    @Test
    void wellFormedInboundRequestIdIsKeptAndEchoedOnTheResponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/x")
                .header("X-Request-Id", "trace-abc.123").build());

        HttpHeaders forwarded = run(exchange, null);

        assertThat(forwarded.getFirst("X-Request-Id")).isEqualTo("trace-abc.123");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Request-Id")).isEqualTo("trace-abc.123");
    }

    @Test
    void missingOrMalformedRequestIdIsReplacedWithAGeneratedOne() {
        HttpHeaders missing = run(MockServerWebExchange.from(MockServerHttpRequest.get("/api/x").build()), null);
        assertThat(missing.getFirst("X-Request-Id")).isNotBlank();

        HttpHeaders malformed = run(MockServerWebExchange.from(MockServerHttpRequest.get("/api/x")
                .header("X-Request-Id", "bad id\twith spaces").build()), null);
        assertThat(malformed.getFirst("X-Request-Id")).isNotEqualTo("bad id\twith spaces").isNotBlank();
    }

    // ---------------------------------------------------------------- helpers

    private HttpHeaders run(ServerWebExchange exchange, JwtAuthenticationToken authentication) {
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        GatewayFilterChain chain = e -> {
            seen.set(e);
            return Mono.empty();
        };

        Mono<Void> result = filter.filter(exchange, chain);
        if (authentication != null) {
            result = result.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
        }
        result.block();

        return seen.get().getRequest().getHeaders();
    }

    private JwtAuthenticationToken authenticated(Long userId, String email, String... authorities) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(email)
                .claim("email", email)
                .claim("userId", userId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        return new JwtAuthenticationToken(jwt, List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}

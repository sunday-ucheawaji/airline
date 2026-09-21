package com.sunday.cloud.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns the validated JWT into the identity headers downstream services trust
 * ({@code X-User-Id / X-User-Email / X-User-Roles}), and tags every request with an {@code X-Request-Id}.
 *
 * <p>Inbound copies of those headers are <em>always</em> stripped first — including on public routes —
 * so a client can never pass an identity through the gateway; downstream only ever sees values the
 * gateway derived from a token it verified itself.
 */
@Component
public class IdentityHeadersGlobalFilter implements GlobalFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String REQUEST_ID = "X-Request-Id";

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = firstNonBlank(exchange.getRequest().getHeaders().getFirst(REQUEST_ID));

        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(JwtAuthenticationToken.class::cast)
                .map(auth -> withIdentity(exchange, auth, requestId))
                .defaultIfEmpty(withoutIdentity(exchange, requestId))
                .flatMap(mutated -> {
                    mutated.getResponse().getHeaders().set(REQUEST_ID, requestId);
                    return chain.filter(mutated);
                });
    }

    private ServerWebExchange withIdentity(ServerWebExchange exchange, Authentication auth, String requestId) {
        Jwt jwt = ((JwtAuthenticationToken) auth).getToken();
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .collect(Collectors.joining(","));

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    stripIdentity(headers);
                    headers.set(USER_ID, String.valueOf(jwt.getClaims().get("userId")));
                    headers.set(USER_EMAIL, String.valueOf(jwt.getClaimAsString("email")));
                    headers.set(USER_ROLES, roles);
                    headers.set(REQUEST_ID, requestId);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange withoutIdentity(ServerWebExchange exchange, String requestId) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    stripIdentity(headers);
                    headers.set(REQUEST_ID, requestId);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private void stripIdentity(org.springframework.http.HttpHeaders headers) {
        headers.remove(USER_ID);
        headers.remove(USER_EMAIL);
        headers.remove(USER_ROLES);
    }

    /** Keeps a well-formed inbound id so traces can span callers; anything else (blank, oversized, odd characters) is replaced. */
    private String firstNonBlank(String inbound) {
        return (inbound != null && SAFE_REQUEST_ID.matcher(inbound).matches()) ? inbound : UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

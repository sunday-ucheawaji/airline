package com.sunday.cloud.config;

import com.sunday.cloud.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class GatewaySecurityFilters {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    @Bean
    public GatewayFilter jwtAuthFilter() {

        return (exchange, chain) -> {

            String authHeader = exchange
                    .getRequest()
                    .getHeaders()
                    .getFirst(JwtConstant.JWT_HEADER);


            if (authHeader == null || !authHeader.startsWith(JwtConstant.TOKEN_PREFIX)) {

                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(JwtConstant.TOKEN_PREFIX.length());

            if (!jwtUtil.isTokenValid(token)) {

                return unauthorized(exchange, "Invalid or expired JWT token");
            }

            return blacklistService
                    .isBlacklisted(token)

                    .flatMap(isBlacklisted -> {

                        if (isBlacklisted) {

                            return unauthorized(exchange, "Token has been revoked");
                        }

                        String email = jwtUtil.extractEmail(token);

                        String authorities = jwtUtil.extractAuthorities(token);

                        Long userId = jwtUtil.extractUserId(token);

                        ServerHttpRequest request = exchange
                            .getRequest()
                            .mutate()
                            .header("X-User-Id", String.valueOf(userId))
                            .header("X-User-Email", email)
                            .header("X-User-Roles", authorities)
                            .build();

                        ServerWebExchange mutatedExchange = exchange
                            .mutate()
                            .request(request)
                            .build();

                        return chain.filter(mutatedExchange);
                    });
        };
    }


    // ============================================================
    // SYSTEM ADMIN AUTHORIZATION
    // ============================================================

    @Bean
    public GatewayFilter systemAdminFilter() {

        return (exchange, chain) -> {

            String roles = exchange
                    .getRequest()
                    .getHeaders()
                    .getFirst("X-User-Roles");

            if (roles == null ||
                    !roles.contains("ROLE_SYSTEM_ADMIN")) {

                return forbidden(
                        exchange,
                        "Access denied. Required role: ROLE_SYSTEM_ADMIN"
                );
            }

            return chain.filter(exchange);
        };
    }


    // ============================================================
    // ERROR RESPONSES
    // ============================================================

    private Mono<Void> unauthorized(
            ServerWebExchange exchange,
            String message
    ) {

        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse()
                .setComplete();
    }


    private Mono<Void> forbidden(
            ServerWebExchange exchange,
            String message
    ) {

        exchange.getResponse()
                .setStatusCode(HttpStatus.FORBIDDEN);

        return exchange.getResponse()
                .setComplete();
    }
}
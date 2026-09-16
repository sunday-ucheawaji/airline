package com.sunday.cloud.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

@Configuration
public class RouteConfig {

    @Bean
    public RedisRateLimiter ApiGatewayRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }

    public KeyResolver userKeyResolver() {

        return exchange -> {

            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id");

            if (userId == null || userId.isBlank()) {
                return Mono.just("anonymous");
            }

            return Mono.just("user:" + userId);
        };
    }

    public KeyResolver ipKeyResolver() {

        return exchange -> {

            String forwardedFor = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");

            if (forwardedFor == null || forwardedFor.isBlank()) {
                return Mono.just("ip:unknown");
            }

            String clientIp = forwardedFor.split(",")[0].trim();

            return Mono.just("ip:" + clientIp);
        };
    }


    // ============================================================
    // ROUTES
    // ============================================================

    @Bean
    public RouteLocator gatewayRouter(
            RouteLocatorBuilder builder,
            GatewayFilter jwtAuthFilter,
            GatewayFilter systemAdminFilter
    ) {

        return builder.routes()


                // ==================================================
                // PUBLIC AUTH ROUTES
                // ==================================================

                .route("auth-routes", r -> r

                        .path("/auth/**")

                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(ipKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("user-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )

                        )

                        .uri("lb://user-service")
                )


                // ==================================================
                // ADMIN LOCATION ROUTES
                // ==================================================

                .route("admin-location-routes", r -> r
                        .predicate(exchange -> {

                            String path = exchange
                                    .getRequest()
                                    .getPath()
                                    .value();

                            HttpMethod method = exchange
                                    .getRequest()
                                    .getMethod();

                            boolean validPath = path.startsWith("/api/cities/")
                                            || path.startsWith("/api/airports/");

                            return HttpMethod.POST.equals(method)
                                    && validPath;
                        })
                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .filter(systemAdminFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("location-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )
                        .uri("lb://location-service")
                )


                // ==================================================
                // ADMIN AIRLINE ROUTE
                // ==================================================

                .route("admin-airline-core-routes", r -> r
                        .predicate(exchange -> {

                            String path = exchange
                                    .getRequest()
                                    .getPath()
                                    .value();

                            HttpMethod method = exchange
                                    .getRequest()
                                    .getMethod();

                            return HttpMethod.GET.equals(method)
                                    && path.equals("/api/airlines");
                        })
                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .filter(systemAdminFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("airline-core-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )
                        .uri("lb://airline-core-service")
                )


                // ==================================================
                // USER SERVICE
                // ==================================================

                .route("user-service-routes", r -> r

                        .path("/api/users/**")

                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("user-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )

                        .uri("lb://user-service")
                )


                // ==================================================
                // AIRLINE CORE
                // ==================================================

                .route("airline-core-routes", r -> r

                        .path(
                                "/api/airlines/**",
                                "/api/aircrafts/**"
                        )

                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("airline-core-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )

                        .uri("lb://airline-core-service")
                )


                // ==================================================
                // SEAT SERVICE
                // ==================================================

                .route("seat-service-routes", r -> r

                        .path(
                                "/api/cabin-classes/**",
                                "/api/seat-maps/**",
                                "/api/seats/**",
                                "/api/seat-instances/**",
                                "/api/flight-instance-cabins/**"
                        )

                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("seat-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )

                        .uri("lb://seat-service")
                )


                // ==================================================
                // FLIGHT OPS
                // ==================================================

                .route("flight-ops-routes", r -> r

                        .path(
                                "/api/flights/**",
                                "/api/flight-instances/**",
                                "/api/flight-schedules/**"
                        )

                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("flight-ops-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )

                        .uri("lb://flight-ops-service")
                )


                // ==================================================
                // PRICING
                // ==================================================

                .route("pricing-service-routes", r -> r

                        .path(
                                "/api/fares/**",
                                "/api/fare-rules/**",
                                "/api/baggage-policies/**"
                        )

                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("pricing-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )
                        .uri("lb://pricing-service")
                )


                // ==================================================
                // ANCILLARY
                // ==================================================

                .route("ancillary-service-routes", r -> r

                        .path(
                                "/api/meals/**",
                                "/api/ancillaries/**",
                                "/api/insurance-coverages/**",
                                "/api/flight-meals/**",
                                "/api/flight-cabin-ancillaries/**"
                        )

                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("ancillary-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )

                        .uri("lb://ancillary-service")
                )


                // ==================================================
                // LOCATION SERVICE
                // ==================================================

                .route("location-service-routes", r -> r

                        .path(
                                "/api/cities/**",
                                "/api/airports/**"
                        )

                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("location-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )

                        .uri("lb://location-service")
                )


                // ==================================================
                // BOOKING
                // ==================================================

                .route("booking-service-routes", r -> r

                        .path("/api/bookings/**")

                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("booking-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )

                        .uri("lb://booking-service")
                )


                // ==================================================
                // PAYMENT
                // ==================================================

                .route("payment-service-routes", r -> r

                        .path("/api/payments/**")

                        .filters(f -> f
                                .filter(jwtAuthFilter)
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(ApiGatewayRateLimiter())
                                        .setKeyResolver(userKeyResolver())
                                )
                                .circuitBreaker(config -> config
                                        .setName("payment-service-cb")
                                        .setFallbackUri("forward:/fallback")
                                )
                        )

                        .uri("lb://payment-service")
                )


                // ==================================================
                // EUREKA
                // ==================================================

                .route("eureka-server", r -> r

                        .path("/eureka/main")

                        .filters(f -> f
                                .rewritePath(
                                        "/eureka/main",
                                        "/"
                                )
                        )

                        .uri("http://localhost:8761")
                )
                .route("eureka-server-static", r -> r
                        .path("/eureka/**")
                        .uri("http://localhost:8761")
                )
                .build();
    }
}
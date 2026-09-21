package com.sunday.cloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;


@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            CorsConfigurationSource corsConfigurationSource,
            @Value("${gateway.security.admin-authority:ROLE_GDS_ADMIN}") String admin) {

        ServerAuthenticationEntryPoint unauthorized = (exchange, ex) -> {
            exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            return ErrorResponses.write(exchange, HttpStatus.UNAUTHORIZED,
                    "Authentication is required, or the access token is missing, invalid or expired");
        };
        ServerAccessDeniedHandler forbidden = (exchange, ex) ->
                ErrorResponses.write(exchange, HttpStatus.FORBIDDEN, "You do not have permission to access this resource");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .oauth2ResourceServer(o -> o
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden)
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(authoritiesConverter())))
                .authorizeExchange(ex -> ex
                        // --- public ---
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/auth/**", "/fallback").permitAll()

                        // --- platform admin ---
                        .pathMatchers("/api/admin/**").hasAuthority(admin)
                        .pathMatchers("/api/roles/**", "/api/permissions/**").hasAuthority(admin)
                        .pathMatchers(HttpMethod.GET, "/api/airlines").hasAuthority(admin)
                        .pathMatchers(HttpMethod.POST,
                                "/api/airlines/*/activate",
                                "/api/airlines/*/suspend",
                                "/api/airlines/*/ban").hasAuthority(admin)
                        .pathMatchers(HttpMethod.GET,
                                "/api/users",
                                "/api/users/{id:\\d+}",
                                "/api/users/{id:\\d+}/roles").hasAuthority(admin)
                        .pathMatchers(HttpMethod.POST, "/api/cities/**", "/api/airports/**").hasAuthority(admin)
                        .pathMatchers(HttpMethod.PUT, "/api/cities/**", "/api/airports/**").hasAuthority(admin)
                        .pathMatchers(HttpMethod.PATCH, "/api/cities/**", "/api/airports/**").hasAuthority(admin)
                        .pathMatchers(HttpMethod.DELETE, "/api/cities/**", "/api/airports/**").hasAuthority(admin)

                        // --- any signed-in user (per-airline checks happen inside the services) ---
                        .pathMatchers("/api/**").authenticated()

                        // --- everything else (incl. /actuator, /eureka) is not reachable through the gateway ---
                        .anyExchange().denyAll())
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer) {

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }

        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withSecretKey(new SecretKeySpec(secretBytes, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer));
        decoder.setJwtValidator(validator);
        return decoder;
    }

    /** user-service already emits fully-formed authorities (e.g. {@code ROLE_GDS_ADMIN}) as a JSON array. */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> authoritiesConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("authorities");
        authorities.setAuthorityPrefix("");

        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new ReactiveJwtGrantedAuthoritiesConverterAdapter(authorities));
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {

        if (allowedOrigins.isEmpty() || allowedOrigins.stream().anyMatch(String::isBlank)) {
            throw new IllegalStateException("app.cors.allowed-origins must list at least one origin");
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setExposedHeaders(List.of("X-Request-Id"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

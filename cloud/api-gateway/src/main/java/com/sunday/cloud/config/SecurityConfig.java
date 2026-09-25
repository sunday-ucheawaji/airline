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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.sunday.cloud.security.GatewayPermissions.*;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            CorsConfigurationSource corsConfigurationSource) {

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

                        // --- applicant: own onboarding application ---
                        .pathMatchers(HttpMethod.POST, "/api/onboarding/applications")
                                .hasAuthority(ONBOARDING_APPLICATION_CREATE)
                        .pathMatchers(HttpMethod.GET, "/api/onboarding/applications", "/api/onboarding/applications/{id:\\d+}")
                                .hasAuthority(ONBOARDING_APPLICATION_READ_OWN)
                        .pathMatchers(HttpMethod.PATCH, "/api/onboarding/applications/{id:\\d+}")
                                .hasAuthority(ONBOARDING_APPLICATION_UPDATE_OWN)
                        .pathMatchers(HttpMethod.POST, "/api/onboarding/applications/{id:\\d+}/submit")
                                .hasAuthority(ONBOARDING_APPLICATION_SUBMIT)
                        .pathMatchers("/api/onboarding/**").denyAll()

                        // --- staff: onboarding review, approval and provisioning ---
                        .pathMatchers(HttpMethod.GET, "/api/admin/onboarding/applications", "/api/admin/onboarding/applications/{id:\\d+}")
                                .hasAnyAuthority(ONBOARDING_APPLICATION_READ, AIRLINE_CREATE)
                        .pathMatchers(HttpMethod.GET, "/api/admin/onboarding/applications/{id:\\d+}/reviews")
                                .hasAnyAuthority(ONBOARDING_APPLICATION_READ, APPROVAL_HISTORY_READ)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:\\d+}/return")
                                .hasAuthority(ONBOARDING_APPLICATION_RETURN)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:\\d+}/approve")
                                .hasAuthority(ONBOARDING_FINAL_APPROVE)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:\\d+}/reject")
                                .hasAuthority(ONBOARDING_FINAL_REJECT)
                        .pathMatchers(HttpMethod.PUT, "/api/admin/onboarding/applications/{id:\\d+}/owner")
                                .hasAuthority(AIRLINE_ADMIN_ASSIGN)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:\\d+}/provision")
                                .hasAuthority(AIRLINE_CREATE)
                        .pathMatchers("/api/admin/**").denyAll()

                        // --- platform administration ---
                        .pathMatchers(HttpMethod.GET, "/api/airlines").hasAuthority(AIRLINE_READ)
                        .pathMatchers(HttpMethod.POST, "/api/airlines/{id:\\d+}/activate").hasAuthority(AIRLINE_ACTIVATE)
                        .pathMatchers(HttpMethod.POST, "/api/airlines/{id:\\d+}/suspend").hasAuthority(AIRLINE_SUSPEND)
                        .pathMatchers(HttpMethod.POST, "/api/airlines/{id:\\d+}/ban").hasAuthority(AIRLINE_BAN)
                        .pathMatchers(HttpMethod.GET,
                                "/api/users",
                                "/api/users/{id:\\d+}",
                                "/api/users/{id:\\d+}/roles").hasAuthority(USER_READ)
                        .pathMatchers(HttpMethod.POST, "/api/roles/{roleId:\\d+}/users/{userId:\\d+}").hasAuthority(USER_ROLE_ASSIGN)
                        .pathMatchers(HttpMethod.DELETE, "/api/roles/{roleId:\\d+}/users/{userId:\\d+}").hasAuthority(USER_ROLE_REVOKE)
                        .pathMatchers(HttpMethod.GET, "/api/roles/**", "/api/permissions/**").hasAnyAuthority(USER_ROLE_ASSIGN, USER_ROLE_REVOKE)
                        // defining roles/permissions and assigning permissions to roles: ACCESS_MANAGE, which only SUPER_ADMIN holds
                        .pathMatchers("/api/roles/**", "/api/permissions/**").hasAuthority(ACCESS_MANAGE)
                        .pathMatchers(HttpMethod.POST, "/api/cities/**", "/api/airports/**").hasAuthority(LOCATION_MANAGE)
                        .pathMatchers(HttpMethod.PUT, "/api/cities/**", "/api/airports/**").hasAuthority(LOCATION_MANAGE)
                        .pathMatchers(HttpMethod.PATCH, "/api/cities/**", "/api/airports/**").hasAuthority(LOCATION_MANAGE)
                        .pathMatchers(HttpMethod.DELETE, "/api/cities/**", "/api/airports/**").hasAuthority(LOCATION_MANAGE)

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

    /**
     * The token carries two claims: {@code roles} (e.g. {@code SENIOR_APPROVER}) and {@code permissions}
     * (e.g. {@code ONBOARDING_FINAL_APPROVE}). They become Spring authorities here: permissions as plain names
     * (use {@code hasAuthority}), roles with Spring's {@code ROLE_} prefix (use {@code hasRole}).
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> authoritiesConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            claim(jwt, "roles").forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            claim(jwt, "permissions").forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
            return Flux.fromIterable(authorities);
        });
        return converter;
    }

    private static List<String> claim(Jwt jwt, String name) {
        List<String> values = jwt.getClaimAsStringList(name);
        return values == null ? List.of() : values;
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

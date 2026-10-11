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

                        // Payment provider webhooks: Razorpay/Stripe/Paystack carry no platform JWT, so these must
                        // be reachable without one. Signature verification inside payment-service IS the
                        // authentication boundary for these three routes — that's the correct design for webhooks.
                        // /api/payments/verify is the deprecated Razorpay-only alias, kept public for one release
                        // for whatever dashboard webhook URL is still configured against it.
                        .pathMatchers(HttpMethod.POST, "/api/payments/webhooks/**", "/api/payments/verify").permitAll()

                        // --- applicant: own onboarding application ---
                        .pathMatchers(HttpMethod.POST, "/api/onboarding/applications")
                                .hasAuthority(ONBOARDING_APPLICATION_CREATE)
                        .pathMatchers(HttpMethod.GET, "/api/onboarding/applications", "/api/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
                                .hasAuthority(ONBOARDING_APPLICATION_READ_OWN)
                        .pathMatchers(HttpMethod.PATCH, "/api/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
                                .hasAuthority(ONBOARDING_APPLICATION_UPDATE_OWN)
                        .pathMatchers(HttpMethod.POST, "/api/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/submit")
                                .hasAuthority(ONBOARDING_APPLICATION_SUBMIT)
                        .pathMatchers(HttpMethod.POST, "/api/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/withdraw")
                                .hasAuthority(ONBOARDING_APPLICATION_WITHDRAW)
                        .pathMatchers(HttpMethod.GET, "/api/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/information-requests")
                                .hasAuthority(ONBOARDING_APPLICATION_READ_OWN)
                        .pathMatchers(HttpMethod.POST, "/api/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/information-requests/{requestId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/respond")
                                .hasAuthority(ONBOARDING_APPLICATION_UPDATE_OWN)
                        .pathMatchers(HttpMethod.POST, "/api/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/documents")
                                .hasAuthority(DOCUMENT_UPLOAD_OWN)
                        .pathMatchers(HttpMethod.GET, "/api/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/documents")
                                .hasAuthority(DOCUMENT_READ_OWN)
                        .pathMatchers(HttpMethod.GET, "/api/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/documents/{documentId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/download-url")
                                .hasAuthority(DOCUMENT_READ_OWN)
                        .pathMatchers(HttpMethod.DELETE, "/api/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/documents/{documentId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
                                .hasAuthority(DOCUMENT_UPLOAD_OWN)
                        .pathMatchers("/api/onboarding/**").denyAll()

                        // --- staff: onboarding review, approval and provisioning ---
                        .pathMatchers(HttpMethod.GET, "/api/admin/onboarding/applications", "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
                                .hasAnyAuthority(ONBOARDING_APPLICATION_READ, AIRLINE_CREATE)
                        .pathMatchers(HttpMethod.GET, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/reviews")
                                .hasAnyAuthority(ONBOARDING_APPLICATION_READ, APPROVAL_HISTORY_READ)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/return")
                                .hasAuthority(ONBOARDING_APPLICATION_RETURN)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/approve")
                                .hasAuthority(ONBOARDING_FINAL_APPROVE)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/reject")
                                .hasAuthority(ONBOARDING_FINAL_REJECT)
                        .pathMatchers(HttpMethod.PUT, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/owner")
                                .hasAuthority(AIRLINE_ADMIN_ASSIGN)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/provision")
                                .hasAuthority(AIRLINE_CREATE)
                        // --- staff: case ownership, specialist stages, information requests, referral ---
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/claim")
                                .hasAuthority(ONBOARDING_APPLICATION_ASSIGN)
                        .pathMatchers(HttpMethod.PUT, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/case-owner")
                                .hasAuthority(ONBOARDING_APPLICATION_ASSIGN)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/release")
                                .hasAuthority(ONBOARDING_APPLICATION_ASSIGN)
                        // the super-admin-only rule for a takeover is checked in the service against the forwarded roles
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/take-over")
                                .hasAuthority(ONBOARDING_APPLICATION_ASSIGN)
                        .pathMatchers(HttpMethod.GET, "/api/admin/onboarding/reviewers").hasAuthority(ONBOARDING_APPLICATION_ASSIGN)
                        .pathMatchers(HttpMethod.GET, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/case-owner-history")
                                .hasAuthority(ONBOARDING_APPLICATION_READ)
                        .pathMatchers(HttpMethod.GET, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages")
                                .hasAuthority(ONBOARDING_APPLICATION_READ)
                        .pathMatchers(HttpMethod.GET, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/information-requests")
                                .hasAuthority(ONBOARDING_APPLICATION_READ)
                        .pathMatchers(HttpMethod.PUT, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages/{stage:compliance|commercial|technical}/assignee")
                                .hasAuthority(ONBOARDING_APPLICATION_ASSIGN)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages/compliance/approve")
                                .hasAuthority(KYC_APPROVE)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages/compliance/reject")
                                .hasAuthority(KYC_REJECT)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages/compliance/information-requests")
                                .hasAuthority(KYC_REQUEST_INFORMATION)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages/commercial/approve")
                                .hasAuthority(COMMERCIAL_APPROVE)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages/commercial/reject")
                                .hasAuthority(COMMERCIAL_REJECT)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages/commercial/information-requests")
                                .hasAuthority(COMMERCIAL_REQUEST_INFORMATION)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages/technical/approve")
                                .hasAuthority(TECHNICAL_APPROVE)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages/technical/reject")
                                .hasAuthority(TECHNICAL_REJECT)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/stages/technical/information-requests")
                                .hasAuthority(TECHNICAL_REQUEST_INFORMATION)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/information-requests")
                                .hasAuthority(ONBOARDING_APPLICATION_REQUEST_INFORMATION)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/comments")
                                .hasAnyAuthority(ONBOARDING_APPLICATION_COMMENT, COMPLIANCE_COMMENT, COMMERCIAL_COMMENT, TECHNICAL_COMMENT, ONBOARDING_COMMENT)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/refer")
                                .hasAuthority(ONBOARDING_APPLICATION_REVIEW)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/send-back")
                                .hasAuthority(ONBOARDING_REQUEST_INFORMATION)
                        .pathMatchers(HttpMethod.GET, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/documents")
                                .hasAuthority(DOCUMENT_READ)
                        .pathMatchers(HttpMethod.GET, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/documents/{documentId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/download-url")
                                .hasAuthority(DOCUMENT_READ)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/documents/{documentId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/verify")
                                .hasAuthority(DOCUMENT_VERIFY)
                        .pathMatchers(HttpMethod.POST, "/api/admin/onboarding/applications/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/documents/{documentId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/reject")
                                .hasAuthority(DOCUMENT_REJECT)
                        .pathMatchers("/api/admin/**").denyAll()

                        // --- platform administration ---
                        .pathMatchers(HttpMethod.GET, "/api/airlines").hasAuthority(AIRLINE_READ)
                        .pathMatchers(HttpMethod.POST, "/api/airlines/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/activate").hasAuthority(AIRLINE_ACTIVATE)
                        .pathMatchers(HttpMethod.POST, "/api/airlines/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/suspend").hasAuthority(AIRLINE_SUSPEND)
                        .pathMatchers(HttpMethod.POST, "/api/airlines/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/ban").hasAuthority(AIRLINE_BAN)
                        .pathMatchers(HttpMethod.GET,
                                "/api/users",
                                "/api/users/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}",
                                "/api/users/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/roles").hasAuthority(USER_READ)
                        .pathMatchers(HttpMethod.POST, "/api/roles/{roleId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/users/{userId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}").hasAuthority(USER_ROLE_ASSIGN)
                        .pathMatchers(HttpMethod.DELETE, "/api/roles/{roleId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/users/{userId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}").hasAuthority(USER_ROLE_REVOKE)
                        .pathMatchers(HttpMethod.GET, "/api/roles/**", "/api/permissions/**").hasAnyAuthority(USER_ROLE_ASSIGN, USER_ROLE_REVOKE)
                        // defining roles/permissions and assigning permissions to roles: ACCESS_MANAGE, which only SUPER_ADMIN holds
                        .pathMatchers("/api/roles/**", "/api/permissions/**").hasAuthority(ACCESS_MANAGE)
                        .pathMatchers(HttpMethod.POST, "/api/cities/**", "/api/airports/**").hasAuthority(LOCATION_MANAGE)
                        .pathMatchers(HttpMethod.PUT, "/api/cities/**", "/api/airports/**").hasAuthority(LOCATION_MANAGE)
                        .pathMatchers(HttpMethod.PATCH, "/api/cities/**", "/api/airports/**").hasAuthority(LOCATION_MANAGE)
                        .pathMatchers(HttpMethod.DELETE, "/api/cities/**", "/api/airports/**").hasAuthority(LOCATION_MANAGE)

                        // Aircraft: gateway only checks authentication — AIRCRAFT_MANAGE/AIRCRAFT_READ are
                        // checked inside airline-core-service, same layering as /api/airlines/{id} PUT/DELETE.
                        .pathMatchers("/api/aircrafts/**").authenticated()

                        // Flights/schedules/instances: gateway only checks authentication — FLIGHT_*/SCHEDULE_*/
                        // INSTANCE_* permissions are checked inside flight-ops-service, same layering as /api/aircrafts/**.
                        .pathMatchers("/api/flights/**", "/api/flight-instances/**", "/api/flight-schedules/**").authenticated()

                        // Bookings, tickets, passengers: owner and airline checks happen inside booking-service. Customer care
                        // reads a booking through the platform-only support path, with passenger data masked.
                        .pathMatchers(HttpMethod.GET, "/api/bookings/support/**").hasAuthority(BOOKING_SUPPORT_READ)
                        .pathMatchers("/api/bookings/**", "/api/tickets/**", "/api/passengers/**").authenticated()

                        // Fares/rules/baggage: gateway only checks authentication — FARE_* permissions are checked
                        // inside pricing-service against the fare's own airline. /internal/fares/** is never routed.
                        .pathMatchers("/api/fares/**", "/api/fare-rules/**", "/api/baggage-policies/**").authenticated()

                        // Payments: owner and airline checks happen inside payment-service, same layering as
                        // bookings. Customer care/finance reads a payment through the platform-only support path.
                        .pathMatchers(HttpMethod.GET, "/api/payments/support/**").hasAuthority(PAYMENT_SUPPORT_READ)
                        .pathMatchers("/api/payments/**").authenticated()

                        // Notifications: a notification has no airline owner of its own (it's a side effect of a
                        // booking/payment event addressed to the customer) — both checks are platform-only.
                        .pathMatchers(HttpMethod.GET, "/api/notifications/support/**").hasAuthority(NOTIFICATION_SUPPORT_READ)
                        .pathMatchers(HttpMethod.POST, "/api/notifications/*/resend").hasAuthority(NOTIFICATION_RESEND)

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

package com.sunday.cloud.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the real {@link SecurityConfig} (real decoder, real authority converter, real rules) with signed
 * tokens against a catch-all probe controller, so the whole access policy is exercised as a table.
 */
@SpringJUnitConfig
@TestPropertySource(properties = {
        "jwt.secret=" + SecurityConfigTest.SECRET,
        "jwt.issuer=" + SecurityConfigTest.ISSUER,
        "app.cors.allowed-origins=http://localhost:3000"
})
class SecurityConfigTest {

    static final String SECRET = "test-secret-test-secret-test-secret-0123456789";
    static final String ISSUER = "gds-user-service";
    private static final String ADMIN = "ROLE_GDS_ADMIN";

    @Configuration
    @EnableWebFlux
    @Import({SecurityConfig.class, ProbeController.class})
    static class TestApp {
    }

    /** Answers 200 on every path and method, so the only thing deciding the status is the security chain. */
    @RestController
    static class ProbeController {
        @RequestMapping("/**")
        String probe() {
            return "ok";
        }
    }

    private WebTestClient client;

    @BeforeEach
    void setUp(ApplicationContext context) {
        client = WebTestClient.bindToApplicationContext(context)
                .configureClient()
                .build();
    }

    // ---------------------------------------------------------------- access matrix

    static Stream<Arguments> accessMatrix() {
        // method, path, expected status for: [anonymous, normal user, platform admin]
        return Stream.of(
                // public
                row("POST", "/auth/login", 200, 200, 200),
                row("POST", "/auth/logout", 200, 200, 200),

                // any signed-in user
                row("GET", "/api/users/profile", 401, 200, 200),
                row("GET", "/api/onboarding/applications", 401, 200, 200),
                row("POST", "/api/onboarding/applications", 401, 200, 200),
                row("GET", "/api/airlines/mine", 401, 200, 200),
                row("GET", "/api/airlines/dropdown", 401, 200, 200),
                row("GET", "/api/airlines/5", 401, 200, 200),
                row("PUT", "/api/airlines/5", 401, 200, 200),
                row("GET", "/api/cities", 401, 200, 200),
                row("GET", "/api/airports/3", 401, 200, 200),
                row("POST", "/api/bookings", 401, 200, 200),

                // platform admin only
                row("GET", "/api/admin/onboarding/applications", 401, 403, 200),
                row("POST", "/api/admin/onboarding/applications/1/review", 401, 403, 200),
                row("GET", "/api/airlines", 401, 403, 200),
                row("POST", "/api/airlines/5/activate", 401, 403, 200),
                row("POST", "/api/airlines/5/suspend", 401, 403, 200),
                row("POST", "/api/airlines/5/ban", 401, 403, 200),
                row("GET", "/api/users", 401, 403, 200),
                row("GET", "/api/users/7", 401, 403, 200),
                row("GET", "/api/users/7/roles", 401, 403, 200),
                row("GET", "/api/roles", 401, 403, 200),
                row("POST", "/api/roles", 401, 403, 200),
                row("POST", "/api/roles/2/users/7", 401, 403, 200),
                row("DELETE", "/api/roles/2/users/7", 401, 403, 200),
                row("GET", "/api/permissions", 401, 403, 200),
                row("POST", "/api/permissions", 401, 403, 200),
                // regression: the old guard matched "/api/cities/" only, so POST /api/cities and PUT/DELETE slipped through
                row("POST", "/api/cities", 401, 403, 200),
                row("POST", "/api/cities/bulk", 401, 403, 200),
                row("PUT", "/api/cities/1", 401, 403, 200),
                row("DELETE", "/api/cities/1", 401, 403, 200),
                row("POST", "/api/airports", 401, 403, 200),
                row("PUT", "/api/airports/1", 401, 403, 200),
                row("DELETE", "/api/airports/1", 401, 403, 200),

                // never reachable through the gateway
                row("POST", "/eureka/apps/ROGUE", 401, 403, 403),
                row("GET", "/eureka/main", 401, 403, 403),
                row("GET", "/actuator/env", 401, 403, 403),
                row("GET", "/something-unlisted", 401, 403, 403)
        );
    }

    private static Arguments row(String method, String path, int anonymous, int user, int admin) {
        return Arguments.of(method, path, anonymous, user, admin);
    }

    @ParameterizedTest(name = "{0} {1}: anonymous={2}, user={3}, admin={4}")
    @MethodSource("accessMatrix")
    void enforcesAccessPolicy(String method, String path, int anonymous, int user, int admin) throws Exception {
        assertStatus(method, path, null, anonymous);
        assertStatus(method, path, token(List.of()), user);
        assertStatus(method, path, token(List.of(ADMIN)), admin);
    }

    @Test
    void adminRoleIsMatchedExactlyNotBySubstring() throws Exception {
        assertStatus("GET", "/api/admin/onboarding/applications", token(List.of("ROLE_GDS_ADMIN_LOOKALIKE")), 403);
        assertStatus("GET", "/api/admin/onboarding/applications", token(List.of("GDS_ADMIN")), 403);
    }

    // ---------------------------------------------------------------- token validation

    @Test
    void rejectsTokensThatAreNotHs256SignedWithTheSharedSecretForTheRightIssuer() throws Exception {
        String path = "/api/users/profile";

        assertStatus("GET", path, token(List.of()), 200);

        // signed with a different HMAC size than the pinned HS256
        assertStatus("GET", path, sign(JWSAlgorithm.HS512, SECRET, claims(ISSUER, future(), List.of())), 401);
        // signed with the right algorithm but the wrong key
        assertStatus("GET", path, sign(JWSAlgorithm.HS256, SECRET + "-tampered", claims(ISSUER, future(), List.of())), 401);
        // wrong issuer
        assertStatus("GET", path, sign(JWSAlgorithm.HS256, SECRET, claims("someone-else", future(), List.of())), 401);
        // expired
        assertStatus("GET", path, sign(JWSAlgorithm.HS256, SECRET,
                claims(ISSUER, Date.from(Instant.now().minusSeconds(120)), List.of())), 401);
        // alg=none (unsigned)
        assertStatus("GET", path, new PlainJWT(claims(ISSUER, future(), List.of(ADMIN))).serialize(), 401);
        // garbage
        assertStatus("GET", path, "not-a-jwt", 401);
    }

    @Test
    void anUnsignedAdminTokenDoesNotGrantAdminAccess() {
        String unsignedAdmin = new PlainJWT(claims(ISSUER, future(), List.of(ADMIN))).serialize();
        assertStatus("GET", "/api/admin/onboarding/applications", unsignedAdmin, 401);
    }

    // ---------------------------------------------------------------- error shape and CORS

    @Test
    void rejectionsUseTheRepoErrorShape() throws Exception {
        client.get().uri("/api/users/profile")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized")
                .jsonPath("$.path").isEqualTo("/api/users/profile")
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.message").exists();

        client.get().uri("/api/roles")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(List.of()))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.error").isEqualTo("Forbidden")
                .jsonPath("$.path").isEqualTo("/api/roles");
    }

    // The CORS tests drive the real web handler with mock request/response objects rather than WebTestClient:
    // WebTestClient's mock connector answers 403 to any request carrying an Origin header here, even though the
    // identical request through the handler (and a real server) is allowed — a harness quirk, not the config.

    @Test
    void preflightIsAnsweredWithoutAToken(ApplicationContext context) {
        MockServerHttpResponse response = preflight(context, "http://localhost:3000", "/api/admin/onboarding/applications");

        assertThat(response.getStatusCode()).isIn(null, HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:3000");
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET");
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
    }

    @Test
    void preflightFromAnUnlistedOriginIsRefused(ApplicationContext context) {
        MockServerHttpResponse response = preflight(context, "https://evil.example", "/api/users/profile");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    private static MockServerHttpResponse preflight(ApplicationContext context, String origin, String path) {
        MockServerHttpRequest request = MockServerHttpRequest.options("http://localhost" + path)
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .build();
        MockServerHttpResponse response = new MockServerHttpResponse();
        WebHttpHandlerBuilder.applicationContext(context).build().handle(request, response).block();
        return response;
    }

    // ---------------------------------------------------------------- helpers

    private void assertStatus(String method, String path, String bearer, int expected) {
        WebTestClient.RequestHeadersSpec<?> request = client
                .method(HttpMethod.valueOf(method))
                .uri(path);
        if (bearer != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        }
        request.exchange().expectStatus().isEqualTo(expected);
    }

    private static String token(List<String> authorities) throws Exception {
        return sign(JWSAlgorithm.HS256, SECRET, claims(ISSUER, future(), authorities));
    }

    private static JWTClaimsSet claims(String issuer, Date expiry, List<String> authorities) {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("someone@example.com")
                .claim("email", "someone@example.com")
                .claim("userId", 42L)
                .claim("authorities", authorities)
                .issueTime(new Date())
                .expirationTime(expiry)
                .build();
    }

    private static Date future() {
        return Date.from(Instant.now().plusSeconds(900));
    }

    private static String sign(JWSAlgorithm algorithm, String secret, JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(algorithm), claims);
        jwt.sign(new MACSigner(padTo(secret, algorithm).getBytes(StandardCharsets.UTF_8)));
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(algorithm);
        return jwt.serialize();
    }

    /** MACSigner enforces a minimum key size per algorithm (HS512 needs 64 bytes). */
    private static String padTo(String secret, JWSAlgorithm algorithm) {
        int minimum = JWSAlgorithm.HS512.equals(algorithm) ? 64 : 32;
        StringBuilder padded = new StringBuilder(secret);
        while (padded.length() < minimum) {
            padded.append('x');
        }
        return padded.toString();
    }
}

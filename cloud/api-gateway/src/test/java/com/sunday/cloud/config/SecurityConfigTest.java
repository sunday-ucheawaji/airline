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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String SUPER = "SUPER_ADMIN";
    private static final String APPLICANT = "AIRLINE_APPLICANT";
    private static final String OFFICER = "ONBOARDING_OFFICER";
    private static final String APPROVER = "SENIOR_APPROVER";
    private static final String PROVISIONER = "AIRLINE_PROVISIONER";
    private static final String PLATFORM_ADMIN = "GDS_PLATFORM_ADMIN";
    private static final String AUDITOR = "GDS_AUDITOR";

    private static final List<String> ALL_PERSONAS =
            List.of(APPLICANT, OFFICER, APPROVER, PROVISIONER, PLATFORM_ADMIN, AUDITOR, SUPER);

    /** Mirrors the user-service seed for the permissions the gateway enforces; user-service puts them in the token next to the role. */
    private static final Map<String, List<String>> STAFF_AND_APPLICANT_PERMISSIONS = Map.of(
            "AIRLINE_APPLICANT", List.of("ONBOARDING_APPLICATION_CREATE", "ONBOARDING_APPLICATION_READ_OWN",
                    "ONBOARDING_APPLICATION_UPDATE_OWN", "ONBOARDING_APPLICATION_SUBMIT"),
            "ONBOARDING_OFFICER", List.of("ONBOARDING_APPLICATION_READ", "ONBOARDING_APPLICATION_RETURN"),
            "SENIOR_APPROVER", List.of("ONBOARDING_APPLICATION_READ", "ONBOARDING_FINAL_APPROVE", "ONBOARDING_FINAL_REJECT"),
            "AIRLINE_PROVISIONER", List.of("AIRLINE_CREATE", "AIRLINE_ADMIN_ASSIGN", "AIRLINE_ACTIVATE"),
            "GDS_PLATFORM_ADMIN", List.of("USER_READ", "USER_ROLE_ASSIGN", "USER_ROLE_REVOKE", "AIRLINE_READ", "AIRLINE_SUSPEND",
                    "AIRLINE_BAN", "LOCATION_MANAGE"),
            "GDS_AUDITOR", List.of("ONBOARDING_APPLICATION_READ", "AIRLINE_READ", "USER_READ", "APPROVAL_HISTORY_READ"));

    /** SUPER_ADMIN holds every permission there is, including ACCESS_MANAGE which no other role has. */
    private static final Map<String, List<String>> SEEDED_MATRIX = withSuperAdmin(STAFF_AND_APPLICANT_PERMISSIONS);

    private static Map<String, List<String>> withSuperAdmin(Map<String, List<String>> others) {
        List<String> all = new ArrayList<>(others.values().stream().flatMap(List::stream).distinct().toList());
        all.add("ACCESS_MANAGE");
        Map<String, List<String>> matrix = new HashMap<>(others);
        matrix.put("SUPER_ADMIN", all);
        return matrix;
    }

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

    /** Anonymous gets 401; the listed roles get 200; every other signed-in caller (including one with no roles) gets 403. */
    private record Rule(String method, String path, List<String> allowed) {}

    private static Rule rule(String method, String path, String... allowed) {
        return new Rule(method, path, List.of(allowed));
    }

    private static Rule anyone(String method, String path) {
        return new Rule(method, path, ALL_PERSONAS);
    }

    static Stream<Rule> accessMatrix() {
        return Stream.of(
                // public
                anyone("POST", "/auth/login"),
                anyone("POST", "/auth/logout"),

                // any signed-in user
                anyone("GET", "/api/users/profile"),
                anyone("GET", "/api/airlines/mine"),
                anyone("GET", "/api/airlines/dropdown"),
                anyone("GET", "/api/airlines/5"),
                anyone("PUT", "/api/airlines/5"),
                anyone("GET", "/api/cities"),
                anyone("GET", "/api/airports/3"),
                anyone("POST", "/api/bookings"),

                // applicant: own application (staff carry no applicant permissions, except the super admin)
                rule("POST", "/api/onboarding/applications", APPLICANT, SUPER),
                rule("GET", "/api/onboarding/applications", APPLICANT, SUPER),
                rule("GET", "/api/onboarding/applications/3", APPLICANT, SUPER),
                rule("PATCH", "/api/onboarding/applications/3", APPLICANT, SUPER),
                rule("POST", "/api/onboarding/applications/3/submit", APPLICANT, SUPER),
                // anything unlisted under /api/onboarding or /api/admin is denied outright, even for the super admin
                rule("DELETE", "/api/onboarding/applications/3"),

                // staff: onboarding
                rule("GET", "/api/admin/onboarding/applications", OFFICER, APPROVER, PROVISIONER, AUDITOR, SUPER),
                rule("GET", "/api/admin/onboarding/applications/3", OFFICER, APPROVER, PROVISIONER, AUDITOR, SUPER),
                rule("GET", "/api/admin/onboarding/applications/3/reviews", OFFICER, APPROVER, AUDITOR, SUPER),
                rule("POST", "/api/admin/onboarding/applications/3/return", OFFICER, SUPER),
                rule("POST", "/api/admin/onboarding/applications/3/approve", APPROVER, SUPER),
                rule("POST", "/api/admin/onboarding/applications/3/reject", APPROVER, SUPER),
                rule("PUT", "/api/admin/onboarding/applications/3/owner", PROVISIONER, SUPER),
                rule("POST", "/api/admin/onboarding/applications/3/provision", PROVISIONER, SUPER),
                rule("POST", "/api/admin/onboarding/applications/3/review"),
                rule("GET", "/api/admin/something-new"),

                // platform administration
                rule("GET", "/api/airlines", PLATFORM_ADMIN, AUDITOR, SUPER),
                rule("POST", "/api/airlines/5/activate", PROVISIONER, SUPER),
                rule("POST", "/api/airlines/5/suspend", PLATFORM_ADMIN, SUPER),
                rule("POST", "/api/airlines/5/ban", PLATFORM_ADMIN, SUPER),
                rule("GET", "/api/users", PLATFORM_ADMIN, AUDITOR, SUPER),
                rule("GET", "/api/users/7", PLATFORM_ADMIN, AUDITOR, SUPER),
                rule("GET", "/api/users/7/roles", PLATFORM_ADMIN, AUDITOR, SUPER),
                rule("GET", "/api/roles", PLATFORM_ADMIN, SUPER),
                rule("GET", "/api/roles/2/permissions", PLATFORM_ADMIN, SUPER),
                rule("GET", "/api/permissions", PLATFORM_ADMIN, SUPER),
                rule("POST", "/api/roles/2/users/7", PLATFORM_ADMIN, SUPER),
                rule("DELETE", "/api/roles/2/users/7", PLATFORM_ADMIN, SUPER),
                // defining roles/permissions and their grants
                rule("POST", "/api/roles", SUPER),
                rule("POST", "/api/roles/2/permissions", SUPER),
                rule("DELETE", "/api/roles/2/permissions/4", SUPER),
                rule("POST", "/api/permissions", SUPER),

                // locations: reads are open, writes need LOCATION_MANAGE
                // regression: the old guard matched "/api/cities/" only, so POST /api/cities and PUT/DELETE slipped through
                rule("POST", "/api/cities", PLATFORM_ADMIN, SUPER),
                rule("POST", "/api/cities/bulk", PLATFORM_ADMIN, SUPER),
                rule("PUT", "/api/cities/1", PLATFORM_ADMIN, SUPER),
                rule("DELETE", "/api/cities/1", PLATFORM_ADMIN, SUPER),
                rule("POST", "/api/airports", PLATFORM_ADMIN, SUPER),
                rule("PUT", "/api/airports/1", PLATFORM_ADMIN, SUPER),
                rule("DELETE", "/api/airports/1", PLATFORM_ADMIN, SUPER),

                // never reachable through the gateway, even for the super admin
                rule("POST", "/eureka/apps/ROGUE"),
                rule("GET", "/eureka/main"),
                rule("GET", "/actuator/env"),
                rule("GET", "/internal/access/role-permissions"),
                rule("GET", "/something-unlisted")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("accessMatrix")
    void enforcesAccessPolicy(Rule rule) throws Exception {
        assertStatus(rule.method(), rule.path(), null, rule.path().startsWith("/auth/") ? 200 : 401);
        assertStatus(rule.method(), rule.path(), token(List.of()), rule.allowed().size() == ALL_PERSONAS.size() ? 200 : 403);
        for (String persona : ALL_PERSONAS) {
            int expected = rule.allowed().contains(persona) ? 200 : 403;
            assertStatus(rule.method(), rule.path(), tokenFor(persona), expected);
        }
    }

    @Test
    void permissionsAreMatchedExactlyNotBySubstring() throws Exception {
        assertStatus("POST", "/api/admin/onboarding/applications/3/approve", token(List.of("ONBOARDING_FINAL_APPROVE_LOOKALIKE")), 403);
        assertStatus("POST", "/api/admin/onboarding/applications/3/approve", tokenWithRoles(List.of("ONBOARDING_FINAL_APPROVE")), 403);
        assertStatus("POST", "/api/roles", token(List.of("ACCESS_MANAGE_LOOKALIKE")), 403);
    }

    @Test
    void onlyTheSuperAdminHoldsRoleManage() throws Exception {
        for (String persona : ALL_PERSONAS) {
            assertStatus("POST", "/api/roles", tokenFor(persona), SUPER.equals(persona) ? 200 : 403);
        }
    }

    @Test
    void aCallerHoldingSeveralRolesGetsTheirUnion() throws Exception {
        assertStatus("POST", "/api/admin/onboarding/applications/3/approve", tokenFor(OFFICER, APPROVER), 200);
        assertStatus("POST", "/api/admin/onboarding/applications/3/provision", tokenFor(OFFICER, APPROVER), 403);
    }

    @Test
    void aLegacyAuthoritiesClaimIsIgnored() throws Exception {
        JWTClaimsSet legacy = new JWTClaimsSet.Builder()
                .issuer(ISSUER).subject("someone@example.com").claim("userId", 42L)
                .claim("authorities", List.of("ROLE_SUPER_ADMIN", "ONBOARDING_FINAL_APPROVE"))
                .issueTime(new Date()).expirationTime(future()).build();

        assertStatus("POST", "/api/admin/onboarding/applications/3/approve", sign(JWSAlgorithm.HS256, SECRET, legacy), 403);
    }

    @Test
    void aRoleAloneGrantsNothingWithoutItsPermissionsInTheToken() throws Exception {
        assertStatus("POST", "/api/admin/onboarding/applications/3/approve", tokenWithRoles(List.of(APPROVER)), 403);
        assertStatus("POST", "/api/admin/onboarding/applications/3/approve", token(List.of("ONBOARDING_FINAL_APPROVE")), 200);
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
        assertStatus("GET", path, new PlainJWT(claims(ISSUER, future(), List.of(), List.of("ACCESS_MANAGE"))).serialize(), 401);
        // garbage
        assertStatus("GET", path, "not-a-jwt", 401);
    }

    @Test
    void anUnsignedAdminTokenDoesNotGrantAdminAccess() {
        String unsignedAdmin = new PlainJWT(claims(ISSUER, future(), List.of(), List.of("ACCESS_MANAGE"))).serialize();
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

    /** The token user-service would issue for these roles: the roles themselves plus the permissions they grant. */
    private static String tokenFor(String... roles) throws Exception {
        List<String> permissions = new ArrayList<>();
        for (String role : roles) {
            permissions.addAll(SEEDED_MATRIX.getOrDefault(role, List.of()));
        }
        return sign(JWSAlgorithm.HS256, SECRET, claims(ISSUER, future(), List.of(roles), permissions));
    }

    /** A token that carries role names but no permissions at all. */
    private static String tokenWithRoles(List<String> roles) throws Exception {
        return sign(JWSAlgorithm.HS256, SECRET, claims(ISSUER, future(), roles, List.of()));
    }

    private void assertStatus(String method, String path, String bearer, int expected) {
        WebTestClient.RequestHeadersSpec<?> request = client
                .method(HttpMethod.valueOf(method))
                .uri(path);
        if (bearer != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        }
        request.exchange().expectStatus().isEqualTo(expected);
    }

    /** A token that carries only these permissions (no roles). */
    private static String token(List<String> permissions) throws Exception {
        return sign(JWSAlgorithm.HS256, SECRET, claims(ISSUER, future(), permissions));
    }

    private static JWTClaimsSet claims(String issuer, Date expiry, List<String> permissions) {
        return claims(issuer, expiry, List.of(), permissions);
    }

    private static JWTClaimsSet claims(String issuer, Date expiry, List<String> roles, List<String> permissions) {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("someone@example.com")
                .claim("email", "someone@example.com")
                .claim("userId", 42L)
                .claim("roles", roles)
                .claim("permissions", permissions)
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

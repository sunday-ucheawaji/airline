package com.sunday.cloud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the whole gateway (real security chain, real rate limiters, routes parsed from application.yaml) with
 * only the external infrastructure switched off — no Config Server, Eureka or Redis connection is needed.
 * Catches wiring mistakes that per-class tests can't (e.g. ambiguous rate-limiter beans, a mistyped route
 * property prefix that silently yields zero routes).
 */
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "jwt.secret=test-secret-test-secret-test-secret-0123456789",
        "jwt.issuer=gds-user-service"
})
class ApiGatewayApplicationTests {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsAndAllRoutesAreRegisteredFromYaml() {
        List<String> ids = routeLocator.getRoutes().map(route -> route.getId()).collectList().block();

        assertThat(ids).containsExactlyInAnyOrder(
                "auth-routes", "user-service-routes", "airline-core-routes", "seat-service-routes",
                "flight-ops-routes", "pricing-service-routes", "ancillary-service-routes",
                "location-service-routes", "booking-service-routes", "payment-service-routes");
    }

    @Test
    void theFullSecurityChainIsActive() {
        WebTestClient client = WebTestClient.bindToApplicationContext(context).configureClient().build();

        client.get().uri("/api/users/profile").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/admin/onboarding/applications").exchange().expectStatus().isUnauthorized();
        client.post().uri("/eureka/apps/ROGUE").exchange().expectStatus().isUnauthorized();
    }
}

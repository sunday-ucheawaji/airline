package com.sunday.cloud.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashSet;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;

/** Where a route's circuit breaker forwards when its service is down or slow: a 503 problem for the original path. */
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public ProblemDetail fallback(ServerWebExchange exchange) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "This service is temporarily unavailable. Please try again in a moment.");
        problem.setInstance(originalPath(exchange));
        problem.setProperty("code", "SERVICE_UNAVAILABLE");
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    private static URI originalPath(ServerWebExchange exchange) {
        LinkedHashSet<URI> originals = exchange.getAttribute(GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        URI original = originals == null || originals.isEmpty() ? exchange.getRequest().getURI() : originals.iterator().next();
        return URI.create(original.getRawPath());
    }
}

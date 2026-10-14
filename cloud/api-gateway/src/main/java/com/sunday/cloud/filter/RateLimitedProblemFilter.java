package com.sunday.cloud.filter;

import com.sunday.cloud.config.ErrorResponses;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * The gateway's rate limiter answers 429 with an empty body. This gives that answer the same problem body as every
 * other error, plus {@code Retry-After}; the limits refill every second.
 */
@Component
public class RateLimitedProblemFilter implements WebFilter, Ordered {

    static final String DETAIL = "Too many requests. Please wait a moment and try again.";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        ServerHttpResponseDecorator response = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> setComplete() {
                if (getStatusCode() != HttpStatus.TOO_MANY_REQUESTS || isCommitted()) {
                    return super.setComplete();
                }
                getHeaders().set(HttpHeaders.RETRY_AFTER, "1");
                getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
                String body = ErrorResponses.body(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", DETAIL, path);
                DataBuffer buffer = bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                return getDelegate().writeWith(Mono.just(buffer));
            }
        };
        return chain.filter(exchange.mutate().response(response).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

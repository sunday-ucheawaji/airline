package com.sunday.cloud.config;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Writes the repo-wide RFC 9457 problem ({@code application/problem+json}: {@code type, title, status, detail,
 * instance, code, timestamp}, same shape as common-lib's {@code GlobalExceptionHandler}) for rejections produced at
 * the gateway itself. The gateway does not depend on common-lib, and only ever sends fixed messages, so this is
 * hand-built JSON.
 */
public final class ErrorResponses {

    private ErrorResponses() {
    }

    public static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String detail) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        String body = body(status, code, detail, exchange.getRequest().getPath().value());
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static String body(HttpStatus status, String code, String detail, String path) {
        return String.format(
                "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"instance\":\"%s\","
                        + "\"code\":\"%s\",\"timestamp\":\"%s\"}",
                status.getReasonPhrase(), status.value(), escape(detail), escape(path), code, Instant.now());
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

package com.sunday.cloud.config;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Writes the repo-wide error body ({@code timestamp, status, error, message, path}, same shape as
 * common-lib's {@code ErrorResponse}) for rejections produced at the gateway itself. The gateway does
 * not depend on common-lib, and only ever sends fixed messages, so this is hand-built JSON.
 */
final class ErrorResponses {

    private ErrorResponses() {
    }

    static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), escape(message),
                escape(exchange.getRequest().getPath().value()));

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

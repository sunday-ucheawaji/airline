package com.sunday.cloud.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.security.Principal;


/**
 * Rate-limit policies. The routes in application.yaml reference them by explicit bean name, but Spring Cloud
 * Gateway's own RequestRateLimiter factory also injects a single default RateLimiter and KeyResolver by type —
 * so with more than one of each defined, the general-purpose ones must be {@code @Primary} or startup fails
 * with "required a single bean, but 2 were found".
 */
@Configuration
public class RateLimitConfig {

    @Bean
    @Primary
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }

    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    @Bean
    @Primary
    public KeyResolver userKeyResolver(@Qualifier("ipKeyResolver") KeyResolver ipKeyResolver) {
        return exchange -> exchange.getPrincipal()
                .map(Principal::getName)
                .map(name -> "user:" + name)
                .switchIfEmpty(ipKeyResolver.resolve(exchange));
    }

    @Bean
    public KeyResolver ipKeyResolver(@Value("${gateway.trusted-proxy-hops:0}") int trustedProxyHops) {
        if (trustedProxyHops > 0) {
            XForwardedRemoteAddressResolver resolver = XForwardedRemoteAddressResolver.maxTrustedIndex(trustedProxyHops);
            return exchange -> Mono.just("ip:" + hostOf(resolver.resolve(exchange)));
        }
        return exchange -> Mono.just("ip:" + hostOf(exchange.getRequest().getRemoteAddress()));
    }

    private String hostOf(InetSocketAddress address) {
        return (address == null || address.getAddress() == null) ? "unknown" : address.getAddress().getHostAddress();
    }
}

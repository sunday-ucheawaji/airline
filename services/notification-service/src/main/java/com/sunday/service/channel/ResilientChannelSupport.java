package com.sunday.service.channel;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.util.concurrent.Callable;

/** Shared circuit-breaker + rate-limiter wrapping for a channel's outbound call — a down/throttled
 *  vendor degrades to a fast failure instead of hanging the Kafka consumer thread or triggering a
 *  429 storm. Both instances are named after the channel ("email"/"sms"), configured under
 *  resilience4j.circuitbreaker.instances.* / resilience4j.ratelimiter.instances.* in application.yaml. */
abstract class ResilientChannelSupport {

    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;

    protected ResilientChannelSupport(String instanceName, CircuitBreakerRegistry circuitBreakerRegistry,
                                       RateLimiterRegistry rateLimiterRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(instanceName);
        this.rateLimiter = rateLimiterRegistry.rateLimiter(instanceName);
    }

    protected <T> T guarded(Callable<T> call) throws Exception {
        return circuitBreaker.executeCallable(() -> rateLimiter.executeCallable(call));
    }
}

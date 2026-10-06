package com.sunday.services.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class PricingClientFallbackFactory implements FallbackFactory<PricingClient> {

    @Override
    public PricingClient create(Throwable cause) {
        log.error("pricing-service call failed – cabin-filtered search returns no results: {}", cause.getMessage(), cause);
        return request -> Collections.emptyList();
    }
}

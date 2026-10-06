package com.sunday.services.client;

import com.sunday.common_lib.payload.request.FareSearchRequest;
import com.sunday.common_lib.payload.response.FareSearchResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Feign client for the pricing-service.
 *
 * <p>Used during flight search: one call per page returns the cheapest sellable fare for each requested
 * flight departure in the requested cabin.
 */
@FeignClient(name = "pricing-service", fallbackFactory = PricingClientFallbackFactory.class)
public interface PricingClient {

    @PostMapping("/internal/fares/search")
    List<FareSearchResult> searchFares(@RequestBody FareSearchRequest request);
}

package com.sunday.services.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * No fallback on purpose: a fallback would swallow airline-core's 403 and turn a denial into a 503.
 * Feign errors propagate to PricingIntegrationServiceImpl, which maps them explicitly.
 */
@FeignClient(name = "airline-core-service")
public interface AirlineClient {

    @GetMapping("/api/airlines/{airlineId}/permissions")
    List<String> getMyPermissions(@PathVariable Long airlineId, @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/api/airlines/permissions/check")
    void requirePermission(
            @RequestParam("airlineIds") List<Long> airlineIds,
            @RequestParam("permission") String permission,
            @RequestHeader("X-User-Id") Long userId);
}

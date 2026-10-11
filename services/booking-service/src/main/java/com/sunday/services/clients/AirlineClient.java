package com.sunday.services.clients;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/** No fallback: a fallback would hide airline-core's 403 or outage. Errors are mapped by BookingAccessService. */
@FeignClient(name = "airline-core-service")
public interface AirlineClient {

    @GetMapping("/api/airlines/{airlineId}/permissions")
    List<String> getMyPermissions(@PathVariable UUID airlineId, @RequestHeader("X-User-Id") UUID userId);
}

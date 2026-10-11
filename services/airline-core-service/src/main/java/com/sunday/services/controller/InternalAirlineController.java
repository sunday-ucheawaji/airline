package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.services.service.AirlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Service-to-service only: {@code /internal/**} is not routed by the api-gateway. */
@RestController
@RequestMapping("/internal/airlines")
@RequiredArgsConstructor
public class InternalAirlineController {

    private final AirlineService airlineService;

    /** No permission check — for display-enrichment callers (e.g. flight-ops-service) with no real caller identity to forward. */
    @GetMapping("/{id}")
    public AirlineResponse getAirlineById(@PathVariable UUID id) {
        return airlineService.getAirlineByIdInternal(id);
    }

    @PostMapping("/batch")
    public Map<UUID, AirlineResponse> getAirlinesByIds(@RequestBody List<UUID> ids) {
        return airlineService.getAirlinesByIdsInternal(ids);
    }
}

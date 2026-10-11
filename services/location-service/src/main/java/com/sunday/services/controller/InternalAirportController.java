package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.services.service.AirportService;
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
@RequestMapping("/internal/airports")
@RequiredArgsConstructor
public class InternalAirportController {

    private final AirportService airportService;

    /** No permission check — for display-enrichment callers (e.g. flight-ops-service's search/response building) with no real caller identity to forward. */
    @GetMapping("/{id}")
    public AirportResponse getAirportById(@PathVariable UUID id) {
        return airportService.getAirportById(id);
    }

    @PostMapping("/batch")
    public Map<UUID, AirportResponse> getAirportsByIds(@RequestBody List<UUID> ids) {
        return airportService.getAirportsByIds(ids);
    }
}

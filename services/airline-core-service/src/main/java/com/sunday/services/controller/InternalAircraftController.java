package com.sunday.services.controller;

import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.services.service.AircraftService;
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
@RequestMapping("/internal/aircrafts")
@RequiredArgsConstructor
public class InternalAircraftController {

    private final AircraftService aircraftService;

    /** No permission check — for display-enrichment callers (e.g. public flight search) with no real caller identity to forward. */
    @GetMapping("/{id}")
    public AircraftResponse getAircraftById(@PathVariable Long id) {
        return aircraftService.getAircraftByIdInternal(id);
    }

    @PostMapping("/batch")
    public Map<Long, AircraftResponse> getAircraftsByIds(@RequestBody List<Long> ids) {
        return aircraftService.getAircraftsByIdsInternal(ids);
    }
}

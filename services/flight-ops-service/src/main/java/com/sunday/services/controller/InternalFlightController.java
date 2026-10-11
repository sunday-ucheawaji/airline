package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.response.FlightResponse;
import com.sunday.services.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Service-to-service only: {@code /internal/**} is not routed by the api-gateway. */
@RestController
@RequestMapping("/internal/flights")
@RequiredArgsConstructor
public class InternalFlightController {

    private final FlightService flightService;

    @PostMapping("/batch")
    public Map<UUID, FlightResponse> getFlightsByIds(@RequestBody List<UUID> ids) {
        return flightService.getFlightsByIds(ids);
    }
}

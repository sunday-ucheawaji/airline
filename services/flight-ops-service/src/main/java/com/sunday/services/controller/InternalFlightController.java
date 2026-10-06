package com.sunday.services.controller;

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
    public Map<Long, FlightResponse> getFlightsByIds(@RequestBody List<Long> ids) {
        return flightService.getFlightsByIds(ids);
    }
}

package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.common_lib.payload.response.FlightResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "flight-ops-service", fallbackFactory = FlightClientFallbackFactory.class)
public interface FlightClient
{
    @GetMapping("/api/flights/{id}")
    FlightResponse getFlightById(@PathVariable UUID id);

    @GetMapping("/api/flight-instances/{id}")
    FlightInstanceResponse getFlightInstanceResponse(@PathVariable UUID id);

    @PostMapping("/api/flights/batch")
    Map<UUID, FlightResponse> getFlightsByIds(@RequestBody List<UUID> ids);

    @PostMapping("/api/flight-instances/batch")
    Map<UUID, FlightInstanceResponse> getFlightInstancesByIds(@RequestBody List<UUID> ids);
}

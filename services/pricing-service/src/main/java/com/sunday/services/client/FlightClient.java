package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.payload.response.FlightResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/** No fallback: an unreachable flight-ops must fail the write, not let it proceed against an unknown airline. */
@FeignClient(name = "flight-ops-service")
public interface FlightClient {

    @PostMapping("/internal/flights/batch")
    Map<UUID, FlightResponse> getFlightsByIds(@RequestBody List<UUID> ids);
}

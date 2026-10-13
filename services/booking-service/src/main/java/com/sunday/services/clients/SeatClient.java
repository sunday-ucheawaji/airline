package com.sunday.services.clients;

import com.sunday.common_lib.payload.response.SeatConfirmResponse;
import com.sunday.common_lib.payload.request.SeatConfirmRequest;
import java.util.UUID;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Internal seat-service endpoints: a booking's own seats are held and shown without a caller airline permission. */
@FeignClient(name = "seat-service", fallbackFactory = SeatClientFallbackFactory.class)
public interface SeatClient {

    @PostMapping("/internal/seat-instances/hold")
    SeatHoldResponse holdSeats(@RequestBody SeatHoldRequest request);

    @PostMapping("/internal/seat-instances/confirm")
    SeatConfirmResponse confirmSeats(@RequestBody SeatConfirmRequest request);

    @GetMapping("/internal/seat-instances/all")
    List<SeatInstanceResponse> getAllByIds(@RequestParam List<UUID> Ids);
}

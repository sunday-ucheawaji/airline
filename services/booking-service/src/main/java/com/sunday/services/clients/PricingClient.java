package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.PassengerType;
import com.sunday.common_lib.payload.response.FareQuoteResponse;
import com.sunday.common_lib.payload.response.FareResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@FeignClient(name = "pricing-service", fallbackFactory = PricingClientFallbackFactory.class)
public interface PricingClient {

    @GetMapping("/internal/fares/{id}")
    FareResponse getFareById(@PathVariable UUID id);

    @PostMapping("/internal/fares/batch")
    Map<UUID, FareResponse> getFaresByIds(@RequestBody List<UUID> ids);

    @GetMapping("/internal/fares/{id}/quote")
    FareQuoteResponse quoteFare(@PathVariable UUID id,
                                @RequestParam("flightId") UUID flightId,
                                @RequestParam("cabinClass") CabinClassType cabinClass,
                                @RequestParam("passengerType") PassengerType passengerType,
                                @RequestParam("departureDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate);
}

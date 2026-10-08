package com.sunday.services.clients;

import com.sunday.common_lib.enums.CabinClassType;
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

@FeignClient(name = "pricing-service", fallback = PricingClientFallback.class)
public interface PricingClient {

    @GetMapping("/internal/fares/{id}")
    FareResponse getFareById(@PathVariable Long id);

    @PostMapping("/internal/fares/batch")
    Map<Long, FareResponse> getFaresByIds(@RequestBody List<Long> ids);

    @GetMapping("/internal/fares/{id}/quote")
    FareQuoteResponse quoteFare(@PathVariable Long id,
                                @RequestParam("flightId") Long flightId,
                                @RequestParam("cabinClass") CabinClassType cabinClass,
                                @RequestParam("departureDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate);
}

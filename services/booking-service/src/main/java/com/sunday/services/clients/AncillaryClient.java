package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.payload.response.FlightCabinAncillaryResponse;
import com.sunday.common_lib.payload.response.FlightMealResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "ancillary-service", fallbackFactory = AncillaryClientFallbackFactory.class)
public interface AncillaryClient {

    @PostMapping("/api/flight-cabin-ancillaries/price/total")
    BigDecimal calculateAncillariesPrice(
            @RequestParam("flightId") UUID flightId,
            @RequestBody List<UUID> flightCabinAncillaryIds);

    @GetMapping("/api/flight-cabin-ancillaries/all")
    List<FlightCabinAncillaryResponse> getAllByIds(
            @RequestParam List<UUID> Ids);

    @GetMapping("/api/flight-meals/all")
    List<FlightMealResponse> getMealsByIds(
            @RequestParam List<UUID> Ids);

    @PostMapping("/api/flight-meals/price/total")
    BigDecimal calculateMealPrice(
            @RequestParam("flightId") UUID flightId,
            @RequestBody List<UUID> requests);

}

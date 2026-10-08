package com.sunday.services.clients;

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
            @RequestParam("flightId") Long flightId,
            @RequestBody List<Long> flightCabinAncillaryIds);

    @GetMapping("/api/flight-cabin-ancillaries/all")
    List<FlightCabinAncillaryResponse> getAllByIds(
            @RequestParam List<Long> Ids);

    @GetMapping("/api/flight-meals/all")
    List<FlightMealResponse> getMealsByIds(
            @RequestParam List<Long> Ids);

    @PostMapping("/api/flight-meals/price/total")
    BigDecimal calculateMealPrice(
            @RequestParam("flightId") Long flightId,
            @RequestBody List<Long> requests);

}

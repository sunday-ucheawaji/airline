package com.sunday.services.client;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.response.CabinClassResponse;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "seat-service", fallback = SeatClientFallback.class)
public interface SeatClient {

    @GetMapping("/api/cabin-classes/aircraft/{aircraftId}")
    List<CabinClassResponse> getCabinClassesByAircraftId(
            @PathVariable Long aircraftId);

   @GetMapping("/api/cabin-classes/aircraft/{id}/name/{cabinClass}")
   CabinClassResponse getCabinClassByAircraftIdAndName(
            @PathVariable CabinClassType cabinClass,
            @PathVariable Long id
   );

    /** Per-cabin count of seats already sold/held (status != AVAILABLE) for one flight instance — used to validate an aircraft reassignment before committing it. */
    @GetMapping("/api/seat-instances/flight-instance/{flightInstanceId}/status-summary")
    List<CabinSeatStatusResponse> getSeatStatusSummary(@PathVariable Long flightInstanceId);
}

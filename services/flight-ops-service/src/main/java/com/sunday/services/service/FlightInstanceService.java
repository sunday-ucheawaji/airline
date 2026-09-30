package com.sunday.services.service;

import com.sunday.common_lib.payload.request.FlightInstanceRequest;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.services.model.Flight;
import com.sunday.services.model.FlightInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FlightInstanceService {

    FlightInstanceResponse createFlightInstanceWithCabins(
            Long userId,
            FlightInstanceRequest request);

    /**
     * Bulk creation for a caller (namely schedule generation) that has already resolved and
     * permission-checked {@code flight}/{@code aircraft} once — skips the per-item permission
     * check, aircraft lookup and flight lookup that {@link #createFlightInstanceWithCabins} has
     * to do for its own direct (single-item) callers, and does one {@code saveAll} instead of N
     * individual saves.
     */
    List<FlightInstance> createFlightInstancesInBulk(Flight flight, AircraftResponse aircraft, List<FlightInstanceRequest> requests);

    FlightInstanceResponse getFlightInstanceById(Long id);

    Page<FlightInstanceResponse> getByAirlineId(Long userId,
                                                Long airlineId,
                                                Long departureAirportId,
                                                Long arrivalAirportId,
                                                Long flightId,
                                                LocalDate onDate,
                                                Pageable pageable);

    FlightInstanceResponse updateFlightInstance(
            Long id,
            FlightInstanceRequest request,
            Long userId);

    void deleteFlightInstance(Long id, Long userId);

    Map<Long, FlightInstanceResponse> getFlightInstancesByIds(List<Long> ids);

    /** Reassign the aircraft on an already-created instance — validated against seat-service's already-sold seats before being committed. */
    FlightInstanceResponse reassignAircraft(Long id, Long newAircraftId, Long userId);
}

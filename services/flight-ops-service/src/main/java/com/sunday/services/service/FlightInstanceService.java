package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.payload.request.FlightInstanceRequest;
import com.sunday.common_lib.payload.request.FlightInstanceUpdateRequest;
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
            UUID userId,
            FlightInstanceRequest request);

    /**
     * Bulk creation for a caller (namely schedule generation) that has already resolved and
     * permission-checked {@code flight}/{@code aircraft} once — skips the per-item permission
     * check, aircraft lookup and flight lookup that {@link #createFlightInstanceWithCabins} has
     * to do for its own direct (single-item) callers, and does one {@code saveAll} instead of N
     * individual saves.
     */
    List<FlightInstance> createFlightInstancesInBulk(Flight flight, AircraftResponse aircraft, List<FlightInstanceRequest> requests);

    FlightInstanceResponse getFlightInstanceById(UUID id);

    Page<FlightInstanceResponse> getByAirlineId(UUID userId,
                                                UUID airlineId,
                                                UUID departureAirportId,
                                                UUID arrivalAirportId,
                                                UUID flightId,
                                                LocalDate onDate,
                                                Pageable pageable);

    /** Times and operational details only, while the departure is still SCHEDULED or DELAYED. */
    FlightInstanceResponse updateFlightInstance(UUID id, FlightInstanceUpdateRequest request, UUID userId);

    /** Moves along the departure lifecycle; cancelling one with seats sold or held is refused. */
    FlightInstanceResponse changeStatus(UUID id, FlightStatus status, UUID userId);

    /** Refused while any seat is sold or held. */
    void deleteFlightInstance(UUID id, UUID userId);

    /** Deletes departures the caller has already checked have no sales, and tells seat-service to drop their seats. */
    void removeDepartures(List<FlightInstance> departures);

    Map<UUID, FlightInstanceResponse> getFlightInstancesByIds(List<UUID> ids);

    /** Reassign the aircraft on an already-created instance — validated against seat-service's already-sold seats before being committed. */
    FlightInstanceResponse reassignAircraft(UUID id, UUID newAircraftId, UUID userId);
}

package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.FlightInstanceCabinRequest;
import com.sunday.common_lib.payload.response.FlightInstanceCabinResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FlightInstanceCabinService {

    FlightInstanceCabinResponse createFlightInstanceCabin(UUID userId, FlightInstanceCabinRequest request);
    FlightInstanceCabinResponse getFlightInstanceCabinById(UUID userId, UUID id);
    Page<FlightInstanceCabinResponse> getByFlightInstanceId(UUID userId, UUID flightInstanceId, Pageable pageable);
    FlightInstanceCabinResponse getByFlightInstanceIdAndCabinClassId(UUID userId, UUID flightInstanceId, UUID cabinClassId);
    FlightInstanceCabinResponse updateFlightInstanceCabin(UUID userId, UUID id, FlightInstanceCabinRequest request);
    void deleteFlightInstanceCabin(UUID userId, UUID id);
}

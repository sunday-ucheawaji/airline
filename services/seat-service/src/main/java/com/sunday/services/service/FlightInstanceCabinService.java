package com.sunday.services.service;

import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.FlightInstanceCabinRequest;
import com.sunday.common_lib.payload.response.FlightInstanceCabinResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FlightInstanceCabinService {

    FlightInstanceCabinResponse createFlightInstanceCabin(FlightInstanceCabinRequest request) throws ResourceNotFoundException;
    FlightInstanceCabinResponse getFlightInstanceCabinById(Long id);
    Page<FlightInstanceCabinResponse> getByFlightInstanceId(
            Long flightInstanceId, Pageable pageable);
    FlightInstanceCabinResponse getByFlightInstanceIdAndCabinClassId(Long flightInstanceId, Long cabinClassId);
    FlightInstanceCabinResponse updateFlightInstanceCabin(Long id, FlightInstanceCabinRequest request);
    void deleteFlightInstanceCabin(Long id);
}

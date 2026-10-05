package com.sunday.services.service;

import com.sunday.common_lib.payload.request.FlightInstanceCabinRequest;
import com.sunday.common_lib.payload.response.FlightInstanceCabinResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FlightInstanceCabinService {

    FlightInstanceCabinResponse createFlightInstanceCabin(Long userId, FlightInstanceCabinRequest request);
    FlightInstanceCabinResponse getFlightInstanceCabinById(Long userId, Long id);
    Page<FlightInstanceCabinResponse> getByFlightInstanceId(Long userId, Long flightInstanceId, Pageable pageable);
    FlightInstanceCabinResponse getByFlightInstanceIdAndCabinClassId(Long userId, Long flightInstanceId, Long cabinClassId);
    FlightInstanceCabinResponse updateFlightInstanceCabin(Long userId, Long id, FlightInstanceCabinRequest request);
    void deleteFlightInstanceCabin(Long userId, Long id);
}

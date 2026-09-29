package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AircraftRequest;
import com.sunday.common_lib.payload.response.AircraftResponse;

import java.util.List;

public interface AircraftService {

    AircraftResponse getAircraftById(Long id, Long userId);

    List<AircraftResponse> listAircraftsForAirline(Long airlineId, Long userId);

    AircraftResponse createAircraft(AircraftRequest request, Long ownerId);

    AircraftResponse updateAircraft(Long id, AircraftRequest request, Long userId);

    void deleteAircraft(Long id, Long userId);
}

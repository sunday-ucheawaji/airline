package com.sunday.services.service;

import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.AircraftRequest;
import com.sunday.common_lib.payload.response.AircraftResponse;

import java.util.List;

public interface AircraftService {

    AircraftResponse getAircraftById(Long id) throws ResourceNotFoundException;

    List<AircraftResponse> listAllAircraftsByOwner(Long ownerId);

    AircraftResponse createAircraft(AircraftRequest request,
                                    Long ownerId) throws ResourceNotFoundException;

    AircraftResponse updateAircraft(Long id, AircraftRequest request, Long ownerId) throws ResourceNotFoundException;

    void deleteAircraft(Long id) throws ResourceNotFoundException;
}

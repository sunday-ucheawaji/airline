package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AircraftRequest;
import com.sunday.common_lib.payload.response.AircraftResponse;

import java.util.List;
import java.util.Map;

public interface AircraftService {

    AircraftResponse getAircraftById(Long id, Long userId);

    /** No permission check — service-to-service only, for display-enrichment callers with no real caller identity (e.g. public flight search). */
    AircraftResponse getAircraftByIdInternal(Long id);

    /** Batch variant of {@link #getAircraftByIdInternal}: one round trip instead of one per aircraft. */
    Map<Long, AircraftResponse> getAircraftsByIdsInternal(List<Long> ids);

    List<AircraftResponse> listAircraftsForAirline(Long airlineId, Long userId);

    AircraftResponse createAircraft(AircraftRequest request, Long ownerId);

    AircraftResponse updateAircraft(Long id, AircraftRequest request, Long userId);

    void deleteAircraft(Long id, Long userId);
}

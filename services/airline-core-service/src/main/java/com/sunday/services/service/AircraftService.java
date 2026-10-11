package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.AircraftRequest;
import com.sunday.common_lib.payload.response.AircraftResponse;

import java.util.List;
import java.util.Map;

public interface AircraftService {

    AircraftResponse getAircraftById(UUID id, UUID userId);

    /** No permission check — service-to-service only, for display-enrichment callers with no real caller identity (e.g. public flight search). */
    AircraftResponse getAircraftByIdInternal(UUID id);

    /** Batch variant of {@link #getAircraftByIdInternal}: one round trip instead of one per aircraft. */
    Map<UUID, AircraftResponse> getAircraftsByIdsInternal(List<UUID> ids);

    List<AircraftResponse> listAircraftsForAirline(UUID airlineId, UUID userId);

    AircraftResponse createAircraft(AircraftRequest request, UUID ownerId);

    AircraftResponse updateAircraft(UUID id, AircraftRequest request, UUID userId);

    void deleteAircraft(UUID id, UUID userId);
}

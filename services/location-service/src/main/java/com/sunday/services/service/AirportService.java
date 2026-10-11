package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AirportRequest;
import com.sunday.common_lib.payload.response.AirportResponse;

import java.util.UUID;
import java.util.List;
import java.util.Map;

public interface AirportService {

    AirportResponse createAirport(AirportRequest request);
    List<AirportResponse> createBulkAirports(List<AirportRequest> requests);
    AirportResponse getAirportById(UUID id);

    /** Batch variant: one round trip instead of one per airport. */
    Map<UUID, AirportResponse> getAirportsByIds(List<UUID> ids);

    List<AirportResponse> getAllAirports();
    AirportResponse updateAirport(UUID id, AirportRequest request);
    void deleteAirport(UUID id);
    List<AirportResponse> getAirportsByCityId(UUID cityId);
}

package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AirportRequest;
import com.sunday.common_lib.payload.response.AirportBulkCreateResponse;
import com.sunday.common_lib.payload.response.AirportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.List;
import java.util.Map;

public interface AirportService {

    AirportResponse createAirport(AirportRequest request);
    AirportBulkCreateResponse createBulkAirports(List<AirportRequest> requests);
    AirportResponse getAirportById(UUID id);

    /** Batch variant: one round trip instead of one per airport. */
    Map<UUID, AirportResponse> getAirportsByIds(List<UUID> ids);

    Page<AirportResponse> getAllAirports(Pageable pageable);
    AirportResponse updateAirport(UUID id, AirportRequest request);
    void deleteAirport(UUID id);
    List<AirportResponse> getAirportsByCityId(UUID cityId);

    /** Bulk-moves every airport currently in {@code sourceCityId} to {@code targetCityId} in one write. */
    List<AirportResponse> reassignAirportsToCity(UUID sourceCityId, UUID targetCityId);
}
